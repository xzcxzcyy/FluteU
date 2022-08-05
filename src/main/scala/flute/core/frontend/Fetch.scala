package flute.core.frontend

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._
import flute.cache.ICacheIO
import flute.util.BitMode.fromIntToBitModeLong
import flute.cache.top.ICacheWithCore
import flute.core.backend.commit.BranchCommit
import flute.config.CPUConfig
import flute.cache.top.ICacheResp

class FetchIO extends Bundle {
  val ibufferEntries = Vec(decodeWay, Decoupled(new IBEntry))
}

class IBEntry extends Bundle {
  val inst      = UInt(instrWidth.W)
  val addr      = UInt(addrWidth.W)
  val inSlot    = Bool()
  val predictBT = UInt(addrWidth.W)
}

class FetchWithCP0 extends Bundle {
  val intrReq = Input(Bool())
  val eretReq = Input(Bool())
  val epc     = Input(UInt(dataWidth.W))
}

class Fetch extends Module {
  assert(fetchGroupSize == 2)

  private val pcQueueVolume: Int = 8

  val io = IO(new Bundle {
    val withDecode   = new FetchIO()
    val branchCommit = Input(new BranchCommit)
    val iCache       = Flipped(new ICacheWithCore)
    val cp0          = new FetchWithCP0
    val pc           = Output(UInt(addrWidth.W))
  })

  val pc        = RegInit("hbfc00000".U(32.W))
  val bpc       = RegInit(0.U.asTypeOf(Valid(UInt(addrWidth.W))))
  val ib        = Module(new Ibuffer(new IBEntry, ibufferAmount, decodeWay, fetchGroupSize))
  val pcQ       = Module(new Queue(UInt(addrWidth.W), pcQueueVolume, hasFlush = true))
  val respStage = RegInit(0.U.asTypeOf(Valid(new ICacheResp)))
  val slot      = RegInit(0.B)
  val preDecs   = for (i <- 0 until fetchGroupSize) yield { Module(new PreDecode) }

  val extFlush      = io.branchCommit.pcRestore.valid || io.cp0.intrReq || io.cp0.eretReq
  val innerFlush    = Wire(Bool())
  val needFlush     = extFlush || innerFlush
  val ibRoom        = ib.io.space
  val ibPermitCache = ibRoom > 8.U
  val cacheFree     = io.iCache.req.ready
  val pcQEnqReady   = pcQ.io.enq.ready

  val pcRenewal     = pcQ.io.enq.fire
  val cacheReqValid = ibPermitCache && pcQEnqReady
  val pcQEnqValid   = ibPermitCache && cacheFree

  when(io.cp0.intrReq) {
    pc := intrProgramAddr.U(addrWidth.W)
  }.elsewhen(io.cp0.eretReq) {
    pc := io.cp0.epc
  }.elsewhen(io.branchCommit.pcRestore.valid) {
    pc := io.branchCommit.pcRestore.bits
  }.elsewhen(innerFlush) {
    pc := bpc.bits
  }.elsewhen(pcRenewal) {
    pc := Mux(FetchUtil.isLastInst(pc), pc + 4.U, pc + 8.U)
  }
  // 笑死，前面管不着后面的
  io.iCache.req.valid     := cacheReqValid
  io.iCache.req.bits.addr := pc

  pcQ.io.enq.valid := pcQEnqValid
  pcQ.io.enq.bits  := pc
  pcQ.io.deq.ready := respStage.valid
  pcQ.io.flush.get := needFlush

  val insertIntoIb = pcQ.io.deq.fire

  val cacheRespValid = io.iCache.resp.valid
  when(needFlush || (insertIntoIb && !cacheRespValid)) {
    respStage.valid := 0.B
  }.elsewhen(cacheRespValid) {
    respStage := io.iCache.resp
  }

  // control signals
  val resultValid = respStage.valid && pcQ.io.deq.valid

  // data path
  val ibEntries = FetchUtil.getIbEntryCouple(resultValid, respStage.bits, pcQ.io.deq.bits)
  for (i <- 0 until fetchGroupSize) yield {
    preDecs(i).io.instruction.valid := ibEntries(i).valid
    preDecs(i).io.instruction.bits  := ibEntries(i).bits.inst
    preDecs(i).io.pc                := ibEntries(i).bits.addr
    ibEntries(i).bits.predictBT     := preDecs(i).io.out.predictBT
  }
  ibEntries(0).bits.inSlot := slot
  ibEntries(1).bits.inSlot := preDecs(0).io.out.isBranch

  val restMask = WireInit(VecInit(Seq.fill(fetchGroupSize)(1.B)))
  innerFlush := 0.B
  when(slot) {
    when(ibEntries(1).valid && ibEntries(1).bits.addr =/= bpc.bits) {
      innerFlush  := 1.B
      restMask(1) := 0.B
    }
  }.otherwise {
    when(bpc.valid && ibEntries(0).bits.addr =/= bpc.bits) {
      innerFlush  := 1.B
      restMask(0) := 0.B
      restMask(1) := 0.B
    }
  }

  val bpcNotUsed = !ibEntries(1).valid && slot

  when(insertIntoIb && !needFlush) {
    slot := ((ibEntries(1).valid && preDecs(1).io.out.isBranch) ||
      (!ibEntries(1).valid && preDecs(0).io.out.isBranch))

    when(ibEntries(1).valid && preDecs(1).io.out.isBranch) {
      bpc.valid := 1.B
      bpc.bits  := preDecs(1).io.out.predictBT
    }.elsewhen(preDecs(0).io.out.isBranch) {
      bpc.valid := 1.B
      bpc.bits  := preDecs(0).io.out.predictBT
    }.elsewhen(!bpcNotUsed) {
      bpc.valid := 0.B
    }
  }

  when(needFlush) {
    bpc.valid := 0.B
    slot      := 0.B
  }

  for (i <- 0 to 1) {
    ib.io.write(i).valid := ibEntries(i).valid && restMask(i) && insertIntoIb
    ib.io.write(i).bits  := ibEntries(i).bits
  }

  for (i <- 0 to 1) {
    io.withDecode.ibufferEntries(i) <> ib.io.read(i)
  }
  ib.io.flush     := extFlush
  io.iCache.flush := needFlush

  io.pc := pc
}

object FetchUtil {
  def isLastInst(instAddr: UInt): Bool = {
    assert(instAddr.getWidth == addrWidth)
    instAddr(4, 2) === "b111".U
  }

  /**
    * Inflates addr, inst, valid for two ib entry valid bundles.
    *
    * @param resp
    * @param pc
    * @return Incomplete wirings. 
    */
  def getIbEntryCouple(resultValid: Bool, resp: ICacheResp, pc: UInt): Vec[Valid[IBEntry]] = {
    assert(pc.getWidth == 32)
    assert(resp.data.length == 2)
    // Wiring in-complete on purpose.
    val ibEntries = Wire(Vec(2, Valid(new IBEntry)))
    ibEntries(0).valid     := resultValid
    ibEntries(0).bits.addr := pc
    ibEntries(0).bits.inst := resp.data(0)
    ibEntries(1).valid     := resultValid && !isLastInst(pc)
    ibEntries(1).bits.addr := pc + 4.U
    ibEntries(1).bits.inst := resp.data(1)

    ibEntries
  }
}

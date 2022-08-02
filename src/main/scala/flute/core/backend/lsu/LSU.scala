package flute.core.backend.lsu

import chisel3._
import chisel3.util._
import flute.cache.top.DCacheReq
import flute.cache.top.DCacheResp
import flute.core.backend.decode.MicroOp
import flute.util.ValidBundle
import flute.core.components.MuxStageReg
import flute.core.components.Sbuffer
import flute.config.CPUConfig._
import flute.core.components.MuxStageRegMode
import flute.core.backend.decode.LoadMode
import flute.core.backend.decode.StoreMode

class LSUWithDCacheIO extends Bundle {
  val req    = DecoupledIO(new DCacheReq)
  val resp   = Flipped(Valid(new DCacheResp))
  val hazard = Input(Bool())
}

class MemReq extends Bundle {

  /**
    * If store, [[data]] contains rt.
    * If load, [[data]] is retrieved from sb with mask [[valid]]
    */
  val data         = UInt(32.W)
  val addr         = UInt(32.W)
  val loadMode     = UInt(LoadMode.width.W)
  val valid        = Vec(4, Bool())
  val storeMode    = UInt(StoreMode.width.W)
  val robAddr      = UInt(robEntryNumWidth.W)
  val writeRegAddr = UInt(phyRegAddrWidth.W)
}

/**
  * Load & Store unit.
  *
  */
class LSU extends Module {
  val io = IO(new Bundle {
    val dcache   = new LSUWithDCacheIO
    val instr    = Flipped(DecoupledIO(new MicroOp(rename = true)))
    val flush    = Input(Bool())
    val toRob    = ValidIO(new MemReq)
    val sbRetire = Input(Bool())
  })

  val sbuffer = Module(new Sbuffer)
  val s0      = Module(new MuxStageReg(ValidBundle(new MicroOp(rename = true))))
  val opQ     = Module(new Queue(new MemReq, 8, hasFlush = true))
  val respQ   = Module(new Queue(new DCacheResp, 8, hasFlush = true))

  val microOpWire = io.instr
  val memAddr     = s0.io.out.bits.op1.op + s0.io.out.bits.immediate

  sbuffer.io.flush             := io.flush
  sbuffer.io.read.memGroupAddr := memAddr(31, 2)
  sbuffer.io.write.memAddr     := memAddr
  sbuffer.io.write.memData     := s0.io.out.bits.op2.op
  sbuffer.io.write.valid := s0.io.out.valid && s0.io.out.bits.storeMode =/= StoreMode.disable && !io.flush
  sbuffer.io.write.storeMode := s0.io.out.bits.storeMode
  sbuffer.io.retire          := io.sbRetire

  // val intoQFire = queue.io.enq.fire
  val cacheReqReady = io.dcache.req.ready && !io.dcache.hazard
  val queueReady    = opQ.io.enq.ready
  val cacheReqValid =
    s0.io.out.valid && (s0.io.out.bits.loadMode =/= LoadMode.disable) && queueReady && !io.flush
  val queueValid =
    s0.io.out.valid && ((s0.io.out.bits.loadMode =/= LoadMode.disable && cacheReqReady) ||
      (s0.io.out.bits.storeMode =/= StoreMode.disable && sbuffer.io.write.ready))

  opQ.io.enq.valid := queueValid

  opQ.io.enq.bits.data := Mux(
    s0.io.out.bits.loadMode =/= LoadMode.disable,
    sbuffer.io.read.data,
    s0.io.out.bits.op2.op
  )
  opQ.io.enq.bits.addr         := s0.io.out.bits.op1.op + s0.io.out.bits.immediate
  opQ.io.enq.bits.loadMode     := s0.io.out.bits.loadMode
  opQ.io.enq.bits.storeMode    := s0.io.out.bits.storeMode
  opQ.io.enq.bits.robAddr      := s0.io.out.bits.robAddr
  opQ.io.enq.bits.writeRegAddr := s0.io.out.bits.writeRegAddr
  opQ.io.enq.bits.valid        := sbuffer.io.read.valid
  opQ.io.flush.get             := io.flush

  io.dcache.req.valid          := cacheReqValid
  io.dcache.req.bits.storeMode := s0.io.out.bits.storeMode // 这里易错
  io.dcache.req.bits.addr      := memAddr
  io.dcache.req.bits.writeData := 0.U

  respQ.io.enq.valid := io.dcache.resp.valid
  respQ.io.enq.bits  := io.dcache.resp.bits
  respQ.io.flush.get := io.flush

  val reqFires = opQ.io.enq.fire

  when(io.flush || (reqFires && !io.instr.valid)) {
    s0.io.mode := MuxStageRegMode.flush
  }.elsewhen(!reqFires && s0.io.out.valid) {
    s0.io.mode := MuxStageRegMode.stall
  }.otherwise {
    s0.io.mode := MuxStageRegMode.next
  }

  io.instr.ready := reqFires || !s0.io.out.valid
  s0.io.in.bits  := io.instr.bits
  s0.io.in.valid := io.instr.valid

  // LSU指令完成，写入rob entry
  val toRob = WireInit(0.U.asTypeOf(new MemReq))
  val replacedData = VecInit(
    for (i <- 0 until 4) yield {
      Mux(
        opQ.io.deq.bits.valid(i),
        opQ.io.deq.bits.data(i * 8 + 7, i * 8),
        respQ.io.deq.bits.loadData(i * 8 + 7, i * 8),
      )
    }
  )

  val finalLoadData = LSUUtils.getLoadData(opQ.io.deq.bits.loadMode, opQ.io.deq.bits.addr(1, 0), replacedData)

  opQ.io.deq.ready := (opQ.io.deq.bits.storeMode =/= StoreMode.disable) ||
    (opQ.io.deq.bits.loadMode =/= LoadMode.disable && respQ.io.deq.valid)
  respQ.io.deq.ready := (opQ.io.deq.bits.loadMode =/= LoadMode.disable) && opQ.io.deq.valid
  when(opQ.io.deq.fire) {
    when(opQ.io.deq.bits.storeMode =/= StoreMode.disable) {
      toRob := opQ.io.deq.bits
    }.elsewhen(
      opQ.io.deq.bits.loadMode =/= LoadMode.disable
    ) {
      toRob.addr         := opQ.io.deq.bits.addr
      toRob.loadMode     := opQ.io.deq.bits.loadMode
      toRob.robAddr      := opQ.io.deq.bits.robAddr
      toRob.writeRegAddr := opQ.io.deq.bits.writeRegAddr
      toRob.storeMode    := opQ.io.deq.bits.storeMode
      toRob.valid        := opQ.io.deq.bits.valid
      toRob.data         := finalLoadData
    }
  }

  io.toRob.valid := opQ.io.deq.fire
  io.toRob.bits  := toRob
}

object LSUUtils {
  def getLoadData(loadMode: UInt, offset: UInt, replacedData: Vec[UInt]) = {
    assert(loadMode.getWidth == LoadMode.width)
    assert(offset.getWidth == 2)
    assert(replacedData.length == 4)
    assert(replacedData(0).getWidth == 8)
    val loadData = MuxLookup(
      key = loadMode,
      default = Cat(replacedData(3), replacedData(2), replacedData(1), replacedData(0)),
      mapping = Seq(
        LoadMode.byteS -> Cat(Fill(24, replacedData(offset)(7)), replacedData(offset)),
        LoadMode.byteU -> Cat(0.U(24.W), replacedData(offset)),
        LoadMode.halfS -> Cat(Fill(16, replacedData(offset + 1.U)(7)), replacedData(offset + 1.U), replacedData(offset)),
        LoadMode.halfU -> Cat(0.U(16.W), replacedData(offset + 1.U), replacedData(offset)),
        LoadMode.word  -> Cat(replacedData(3), replacedData(2), replacedData(1), replacedData(0)),
      )
    )

    loadData
  }
}

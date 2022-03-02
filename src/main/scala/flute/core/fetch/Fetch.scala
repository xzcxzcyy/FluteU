package flute.core.fetch

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._

import flute.config.CPUConfig._
import flute.cache.ICacheIO
import flute.core.execute.ExecuteFeedbackIO
import flute.core.decode.DecodeFeedbackIO

class FetchIO extends Bundle {
  val insts       = Output(Vec(fetchGroupSize, new IBEntry)) // 指令队列
  val instNum     = Output(UInt(fetchAmountWidth.W)) // 队列指令数量
  val willProcess = Input(UInt(fetchAmountWidth.W))  // 下一拍到来时，decode取走指令条数
}

class IBEntry extends Bundle {
  val inst = UInt(instrWidth.W)
  val addr = UInt(addrWidth.W)
}

class Fetch extends Module {
  val io = IO(new Bundle {
    val withDecode         = new FetchIO()
    val feedbackFromDecode = Flipped(new DecodeFeedbackIO())
    val feedbackFromExec   = Flipped(new ExecuteFeedbackIO())
    val iCache             = Flipped(new ICacheIO())
  })

  val instNum     = RegInit(0.U(fetchAmountWidth.W))
  val pc          = Module(new PC)
  val iB          = RegInit(VecInit(Seq.fill(fetchGroupSize)((new IBEntry).Lit())))
  val preDecoders = for (i <- 0 until fetchGroupSize) yield Module(new PreDecode)

  val bias = pc.io.out(1 + fetchGroupWidth, 1 + 1)

  for (i <- 0 until fetchGroupSize) {
    preDecoders(i).io.instruction.bits  := io.iCache.data.bits(i)
    preDecoders(i).io.instruction.valid := io.iCache.data.valid && (i.U >= bias)
    preDecoders(i).io.pc := Cat(
      pc.io.out(31, fetchGroupWidth + 2),
      i.U(fetchGroupWidth.W),
      0.U(2.W)
    )
  }

  val earliestBranchInd = PriorityEncoder(preDecoders.map(_.io.isBranch))

  val instNumIBPermits = fetchGroupSize.U - instNum + io.withDecode.willProcess
  val instNumCacheLineHas = Mux(
    io.iCache.data.valid,
    fetchGroupSize.U - bias,
    0.U(fetchAmountWidth.W)
  )
  val instNumInc =
    Mux(instNumIBPermits <= instNumCacheLineHas, instNumIBPermits, instNumCacheLineHas)
  instNum     := instNum - io.withDecode.willProcess + instNumInc
  pc.io.stall := !io.iCache.data.valid
  pc.io.in    := pc.io.out + Cat(instNumInc, 0.U(2.W))
  for (i <- 0 until fetchGroupSize) yield {
    when(i.U + io.withDecode.willProcess < instNum) {
      iB(i.U) := iB(i.U + io.withDecode.willProcess)
    }.elsewhen(i.U + io.withDecode.willProcess - instNum < instNumCacheLineHas) {
      iB(i.U).inst := io.iCache.data.bits(
        i.U + io.withDecode.willProcess - instNum + bias
      )
      iB(i.U).addr := Cat(
        pc.io.out(31, fetchGroupWidth + 2),
        i.U + io.withDecode.willProcess - instNum + bias,
        0.U(2.W)
      )
    }.otherwise {
      iB(i.U) := (new IBEntry).Lit()
    }
  }

  io.iCache.addr.bits  := pc.io.out
  io.iCache.addr.valid := 1.B
  io.iCache.data.ready := 1.B

  io.withDecode.instNum := instNum
  io.withDecode.insts   := iB

}

class PC extends Module {
  val io = IO(new Bundle {
    val out   = Output(UInt(addrWidth.W))
    val in    = Input(UInt(addrWidth.W))
    val stall = Input(Bool())
  })
  val data = RegInit(0.U(addrWidth.W))
  when(!io.stall) {
    data := io.in
  }
  io.out := data
}

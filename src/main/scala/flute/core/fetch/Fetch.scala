package flute.core.fetch

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._
import flute.cache.ICacheIO
import flute.core.execute.ExecuteFeedbackIO
import flute.core.decode.DecodeFeedbackIO

class FetchIO extends Bundle {
  // 指令队列，不确定指令用什么格式往下传，需要附带什么信息，暂用32位表示
  val insts       = Output(Vec(fetchGroupSize, UInt(instrWidth.W)))
  val instNum     = Output(UInt(fetchAmountWidth.W)) // 队列指令数量
  val willProcess = Input(UInt(fetchAmountWidth.W))  // 下一拍到来时，decode取走指令条数
}

class Fetch extends Module {
  val io = IO(new Bundle {
    val withDecode         = new FetchIO()
    val feedbackFromDecode = Flipped(new DecodeFeedbackIO())
    val feedbackFromExec   = Flipped(new ExecuteFeedbackIO())
    val iCache             = Flipped(new ICacheIO())
  })

  val instNum = RegInit(0.U(fetchAmountWidth.W))
  val pc      = Module(new PC)
  val insts   = RegInit(VecInit(Seq.fill(fetchGroupSize)(0.U(instrWidth.W))))

  val instNumIBPermits = fetchGroupSize.U - instNum + io.withDecode.willProcess
  val instNumCacheLineHas = Mux(
    io.iCache.data.valid,
    fetchGroupSize.U - pc.io.out(1 + fetchGroupWidth, 1 + 1),
    0.U(fetchAmountWidth.W)
  )
  val instNumInc =
    Mux(instNumIBPermits <= instNumCacheLineHas, instNumIBPermits, instNumCacheLineHas)
  instNum     := instNum - io.withDecode.willProcess + instNumInc
  pc.io.stall := !io.iCache.data.valid
  pc.io.in    := pc.io.out + Cat(instNumInc, 0.U(2.W))
  for (i <- 0 to fetchGroupSize - 1) yield {
    when(i.U + io.withDecode.willProcess < instNum) {
      insts(i.U) := insts(i.U + io.withDecode.willProcess)
    }.elsewhen(i.U + io.withDecode.willProcess - instNum < instNumCacheLineHas) {
      insts(i.U) := io.iCache.data.bits(
        i.U + io.withDecode.willProcess - instNum + pc.io.out(1 + fetchGroupWidth, 1 + 1)
      )
    }.otherwise {
      insts(i.U) := 0.U
    }
  }

  io.iCache.addr.bits  := pc.io.out
  io.iCache.addr.valid := 1.B
  io.iCache.data.ready := 1.B

  io.withDecode.instNum := instNum
  io.withDecode.insts   := insts

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

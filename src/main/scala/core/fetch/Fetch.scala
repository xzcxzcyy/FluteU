package core.fetch

import chisel3._
import chisel3.util.MuxLookup

import config.CpuConfig._
import cache.ICacheIO
import core.execute.ExecuteFeedbackIO

class FetchIO extends Bundle {
  // 指令队列，不确定指令用什么格式往下传，需要附带什么信息，暂用32位表示
  val insts       = Output(Vec(fetchGroupSize, UInt(instrWidth.W)))
  val instNum     = Output(UInt(fetchGroupWidth.W)) // 队列指令数量
  val willProcess = Input(UInt(fetchGroupWidth.W))  // 下一拍到来时，decode取走指令条数
}

class FetchFeedback extends Bundle {
  val executors = Vec(superscalar, new ExecuteFeedbackIO())
}

class Fetch extends Module {
  val io = IO(new Bundle {
    val next     = new FetchIO()
    val feedback = new FetchFeedback()
    val iCache   = new ICacheIO()
  })

  val instNum = RegInit(0.U(fetchGroupWidth.W))
  val pc      = Module(new PC)
  val insts   = RegInit(VecInit(Seq.fill(fetchGroupSize)(0.U(instrWidth.W))))

  val instNumIBPermits = fetchGroupSize.U - instNum + io.next.willProcess
  val instNumCacheLineHas = Mux(
    io.iCache.data.valid,
    fetchGroupSize.U - pc.io.out(1 + fetchGroupWidth, 1 + 1),
    0.U(fetchGroupWidth.W)
  )
  val instNumInc =
    Mux(instNumIBPermits <= instNumCacheLineHas, instNumIBPermits, instNumCacheLineHas)
  instNum     := instNum - io.next.willProcess + instNumInc
  pc.io.stall := io.iCache.data.valid
  pc.io.in    := pc.io.in + instNumInc
  for (i <- 0 to fetchGroupSize - 1) yield {
    when (i.U + 1.U + io.next.willProcess <= instNum) {
      insts(i.U) := insts(i.U + io.next.willProcess)
    } .otherwise {
      insts(i.U) := io.iCache.data.bits(i.U + io.next.willProcess - instNum)
    }
  }
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

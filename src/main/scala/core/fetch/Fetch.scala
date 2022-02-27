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
  
}

package core.fetch

import chisel3._
import chisel3.util.MuxLookup

import config.CpuConfig._
import cache.ICacheIO
import core.execute.ExecuteFeedbackIO

class FetchIO extends Bundle {
  // 指令队列，不确定指令用什么格式往下传，需要附带什么信息，暂用32位表示
  val insts     = Output(Vec(fetchGroupSize, UInt(instrWidth.W)))
  val fetched   = Output(UInt(fetchGroupWidth.W)) // 有效指令长度，不超过8
  val processed = Input(UInt(fetchGroupWidth.W))  // 被Decode取走数量
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

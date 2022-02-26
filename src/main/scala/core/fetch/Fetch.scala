package flute.core.fetch

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.cache.ICacheIO
import flute.core.execute.ExecuteFeedbackIO

class FetchIO(implicit conf: CPUConfig) extends Bundle {
  // 指令队列，不确定指令用什么格式往下传，需要附带什么信息，暂用32位表示
  val insts     = Output(Vec(conf.fetchGroupSize, UInt(conf.instrWidth.W)))
  val fetched   = Output(UInt(conf.fetchGroupWidth.W)) // 有效指令长度，不超过8
  val processed = Input(UInt(conf.fetchGroupWidth.W))  // 被Decode取走数量
}

class FetchFeedback(implicit conf: CPUConfig) extends Bundle {
  val executors = Vec(conf.superscalar, new ExecuteFeedbackIO())
}

class Fetch(implicit conf: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val next     = new FetchIO()
    val feedback = new FetchFeedback()
    val iCache   = new ICacheIO()
  })
}

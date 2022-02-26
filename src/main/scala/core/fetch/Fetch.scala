package flute.core.fetch

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.cache.ICacheIO
import flute.core.execute.ExecuteFeedbackIO

class FetchIO(implicit conf: CPUConfig) extends Bundle {
    val insts       = Vec(8, SInt(32.W)) // 指令队列，不确定指令用什么格式往下传，需要附带什么信息，暂用32位表示
    val fetched     = Output(UInt(3.W))  // 有效指令长度，不超过8
    val processed   = Input(UInt(3.W))  // 被Decode取走数量
}

class FetchFeedback(implicit conf: CPUConfig) extends Bundle {
    val executors = Vec(conf.superscalar, new ExecuteFeedbackIO())
}

class Fetch(implicit conf: CPUConfig) extends Module {
    val io = IO(new Bundle {
        val next        = new FetchIO()
        val feedback    = new FetchFeedback()
        val iCache      = new ICacheIO()
    })
}
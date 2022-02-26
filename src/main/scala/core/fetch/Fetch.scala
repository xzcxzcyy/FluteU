package flute.core.fetch

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.cache.ICacheIO
import flute.core.execute.ExecuteFeedbackIO

class FetchIO(implicit conf: CPUConfig) extends Bundle {
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
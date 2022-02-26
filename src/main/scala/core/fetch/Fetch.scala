package flute.core.fetch

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.cache.ICacheIO

class FetchIO(implicit conf: CPUConfig) extends Bundle {
}

class FetchFeedback(implicit conf: CPUConfig) extends Bundle {
}

class Fetch(implicit conf: CPUConfig) extends Module {
    val io = IO(new Bundle {
        val next        = new FetchIO()
        val feedback    = new FetchFeedback()
        val iCache      = new ICacheIO()
    })
}
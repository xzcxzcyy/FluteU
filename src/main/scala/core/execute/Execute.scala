package flute.core.execute

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.cache.DCacheIO
import flute.core.decode.DecodeIO
import flute.core.fetch.FetchFeedback

class ExecuteIO(implicit conf:CPUConfig) extends Bundle {
}

class Execute(implicit conf:CPUConfig) extends Module {
  val io = IO(new Bundle {
    val decode  = new DecodeIO()
    val fetch   = new FetchFeedback()
    val dCache  = new DCacheIO()
  })
}
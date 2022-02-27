package core.execute

import chisel3._
import chisel3.util.MuxLookup

import config.CPUConfig._
import cache.DCacheIO
import core.decode.DecodeIO

class ExecutorIO extends Bundle {}

class ExecuteFeedbackIO extends Bundle {}

class Execute extends Module {
  val io = IO(new Bundle {
    val withDecode = Flipped(new DecodeIO())
    val feedback   = new ExecuteFeedbackIO()
    val dCache     = Vec(superscalar, Flipped(new DCacheIO()))
  })
}

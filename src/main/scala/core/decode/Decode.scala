package core.decode

import chisel3._
import chisel3.util.MuxLookup

import config.CPUConfig._
import core.fetch.FetchIO
import core.execute.ExecutorIO

class DecodeIO extends Bundle {
  val microOps = Vec(superscalar, new MicroOp)
}

class DecodeFeedbackIO extends Bundle {}

class Decode extends Module {
  val io = IO(new Bundle {
    val withExecute = new DecodeIO()
    val withFetch   = Flipped(new FetchIO())
    val feedback    = new DecodeFeedbackIO()
  })
}

package core.decode

import chisel3._
import chisel3.util.MuxLookup

import config.CPUConfig._
import core.fetch.FetchIO
import core.execute.ExecuteIO

class DecodeIO extends Bundle {
  val executors = Vec(superscalar, new ExecuteIO())
}

class Decode extends Module {
  val io = IO(new Bundle {
    val next  = new DecodeIO()
    val fetch = new FetchIO()
  })
}

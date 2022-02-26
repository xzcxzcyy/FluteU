package flute.core.decode

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.core.fetch.FetchIO

class DecodeIO(implicit conf:CPUConfig) extends Bundle {
}

class Decode(implicit conf:CPUConfig) extends Module {
  val io = IO(new Bundle {
    val next    = new DecodeIO()
    val fetch   = new FetchIO()
  })
}
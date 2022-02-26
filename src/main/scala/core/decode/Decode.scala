package flute.core.decode

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.core.fetch.FetchIO
import flute.core.execute.ExecuteIO

class DecodeIO(implicit conf:CPUConfig) extends Bundle {
    val executors = Vec(conf.superscalar, new ExecuteIO())
}

class Decode(implicit conf:CPUConfig) extends Module {
    val io = IO(new Bundle {
        val next    = new DecodeIO()
        val fetch   = new FetchIO()
    })
}
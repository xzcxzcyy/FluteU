package flute.cache

import flute.config._

import chisel3._
import chisel3.util.MuxLookup

class DCacheIO(implicit conf:CPUConfig) extends Bundle {
    val addr = Input(UInt(conf.addrWidth.W))
    val data  = Output(UInt(conf.dataWidth.W))
}

class DCache(implicit conf:CPUConfig) extends Module {
  val io = IO(new Bundle {
    val port = new DCacheIO()
  })
}
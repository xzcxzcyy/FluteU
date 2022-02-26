package flute.cache

import flute.config._

import chisel3._
import chisel3.util.MuxLookup

class ICacheIO(implicit conf:CPUConfig) extends Bundle {
    val addr = Input(UInt(conf.addrWidth.W))
    val data  = Output(UInt(conf.dataWidth.W))
}

class ICache(implicit conf:CPUConfig) extends Module {
  val io = IO(new Bundle {
    val port = new ICacheIO()
  })
}
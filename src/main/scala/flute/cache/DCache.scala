package flute.cache

import chisel3._
import chisel3.util.MuxLookup
import flute.config.CPUConfig._

class DCacheIO extends Bundle {
  val addr = Input(UInt(addrWidth.W))
  val data = Output(UInt(dataWidth.W))
}

class DCache extends Module {
  val io = IO(new Bundle {
    val port = new DCacheIO()
  })
}

package flute.cache.components

import chisel3._
import chisel3.util._

class ramIO(depth: Int, width: Int) extends Bundle {
  val enable  = Input(Bool())
  val write   = Input(Bool())
  val addr    = Input(UInt(log2Ceil(depth).W))
  val dataIn  = Input(UInt(width.W))
  val dataOut = Output(UInt(width.W))
}

class SinglePortRam(depth: Int, width: Int) extends Module {
  val io = IO(new ramIO(depth, width))
  require(isPow2(depth))

  val mem = SyncReadMem(depth, UInt(width.W))
  io.dataOut := DontCare
  when(io.enable) {
    val rdwrPort = mem(io.addr)
    when(io.write) { rdwrPort := io.dataIn }
      .otherwise { io.dataOut := rdwrPort }
  }
}

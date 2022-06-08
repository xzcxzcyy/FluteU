package flute.cache.ram

import chisel3._
import chisel3.util._
import flute.cache.components.SinglePortRam

class TypedRamIO[T <: Data](depth: Int, gen: T) extends Bundle {
  require(gen.getWidth > 0)
  val write   = Input(Bool())
  val addr    = Input(UInt(log2Ceil(depth).W))
  val dataIn  = Input(gen)
  val dataOut = Output(gen)
}

class TypedSinglePortRam[T <: Data](depth: Int, gen: T) extends Module {
  val io = IO(new TypedRamIO(depth, gen))

  def getPort() = new TypedRamIO(depth, gen)
  val width = gen.getWidth

  val ram = Module(new SinglePortRam(depth, width))

  ram.io.enable := 1.B
  ram.io.addr := io.addr
  ram.io.write := io.write
  ram.io.dataIn := io.dataIn.asUInt
  io.dataOut := ram.io.dataOut.asTypeOf(gen)
}

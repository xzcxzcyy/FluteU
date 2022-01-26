package components


import chisel3._
import config.CpuConfig._
import chisel3.util.MuxLookup

class Comparator extends Module{
  class Flag extends Bundle {
    val equal = Bool()
    val lessU = Bool()
    val lessS = Bool()
  }

  val io = IO(new Bundle {
    val x     = Input(UInt(dataWidth.W))
    val y     = Input(UInt(dataWidth.W))
    val flag  = Output(new Flag())
  })

  io.flag.equal := io.x === io.y
  io.flag.lessS := io.x.asSInt < io.y.asSInt
  io.flag.lessU := io.x.asUInt < io.y.asUInt
}

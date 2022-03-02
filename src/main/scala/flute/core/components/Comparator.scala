package flute.core.components

import chisel3._
import flute.config.CPUConfig._
import chisel3.util.MuxLookup

class Comparator extends Module{
  class Flag extends Bundle {
    val equal = Bool()
    val lessU = Bool()
    val lessS = Bool()
  }

  class BranchSig extends Bundle {
    val rsSignBit = Bool()
    val rsGtz = Bool()
  }

  val io = IO(new Bundle {
    val x           = Input(UInt(dataWidth.W))
    val y           = Input(UInt(dataWidth.W))
    val flag        = Output(new Flag())
    val branchSig   = Output(new BranchSig())
  })

  io.flag.equal := io.x === io.y
  io.flag.lessS := io.x.asSInt < io.y.asSInt
  io.flag.lessU := io.x.asUInt < io.y.asUInt
  io.branchSig.rsSignBit := io.x(dataWidth - 1).asBool
  io.branchSig.rsGtz     := !io.x(dataWidth - 1).asBool && (io.x =/= 0.asUInt(dataWidth.W))
}

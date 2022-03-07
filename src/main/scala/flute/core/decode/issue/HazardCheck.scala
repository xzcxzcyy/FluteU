package flute.core.decode.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp

class DecoupledSig extends Bundle {
  val valid = Output(Bool())
  val ready = Input(Bool())
}

class HazardCheck extends Module {
  val io = IO(new Bundle {
    val in  = Vec(2, Flipped(DecoupledIO(new MicroOp)))
    val out = Vec(2, new DecoupledSig)

    val query = Flipped(new QueryWBIO)
  })
  for (i <- 0 until 4) io.query.addrIn(i) := i.U
  for (i <- 0 until 2) {
    io.in(i).ready  := 0.B
    io.out(i).valid := 0.B
  }
}

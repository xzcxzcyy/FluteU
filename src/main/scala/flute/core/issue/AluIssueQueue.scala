package flute.core.issue

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.decode.MicroOp

class AluIssueQueue extends Module {
  val io = IO(new Bundle {
    val enq = Vec(2, Flipped(DecoupledIO(new MicroOp)))
    val deq = Vec(2, DecoupledIO(new MicroOp))
  })
}

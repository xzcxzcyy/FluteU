package flute.util

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Experiment extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Priority encoder test" in {
    test(new EncoderTester) { c =>
      c.io.in.poke("b0000".U)
      c.io.out.expect(3.U)
    }
  }
}

class EncoderTester extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(4.W))
    val out = Output(UInt(4.W))
  })

  io.out := PriorityEncoder(io.in)
}

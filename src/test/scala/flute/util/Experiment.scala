package flute.util

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/**
  * Experiment
  * Anyone can place codes here for API verification use.
  */
class Experiment extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Priority encoder test" in {
    test(new EncoderTester) { c =>
      c.io.in.poke("b0000".U)
      c.io.out.expect(3.U)
    }
  }

  "Sign extender test" in {
    test(new SExtTester) { c =>
      c.io.in.poke(0xfffe.U)
      c.io.out.expect(0xfffffffeL.U)
    }
  }

  "MuxCase test" in {
    test(new MuxCaseTester) { c =>
      c.io.out.expect(3.U)
    }
  }

  "Reassgin test" in {
    test(new ReassignTester) {c =>
      c.io.in.a.poke(0.B)

      c.io.out.a.expect(1.B)
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

class SExtTester extends Module {
  val io = IO(new Bundle{
    val out = Output(UInt(32.W))
    val in  = Input(UInt(16.W))
  })
  
  io.out := Cat(Fill(16, io.in(15)), io.in(15, 0))
}

class MuxCaseTester extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(5.W))

  })
  io.out := MuxCase(0.U, Seq(
    0.B -> 1.U,
    0.B -> 2.U,
    1.B -> 3.U,
    1.B -> 4.U,
  ))

}

class ReassignTestBundle extends Bundle {
  val a = Bool()
}
class ReassignTester extends Module {
  val io = IO(new Bundle {
    val in = Input(new ReassignTestBundle)
    val out = Output(new ReassignTestBundle)
  })

  val t = Wire(Output(new ReassignTestBundle))
  t.a := 1.B
  io.out := t
}
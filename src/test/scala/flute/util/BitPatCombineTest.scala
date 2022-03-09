package flute.util

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.config.{BasicInstructions, Instructions}
import flute.config.CPUConfig._
import flute.util.BitPatCombine

class BitPatCombineTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "General test" in {
    test(new BitPatCombineTester) { c =>
      c.io.instruction.poke("b00001000000000000000000000000000".U)
      c.io.isJ.expect(1.B)
      c.io.instruction.poke(0.U)
      c.io.isJ.expect(0.B)
    }
  }
}

class BitPatCombineTester extends Module {

  private object instrs extends BasicInstructions {
    def isJOp = BitPatCombine(Seq(J, JAL, JR))
  }

  val io = IO(new Bundle {
    val instruction = Input(UInt(instrWidth.W))
    val isJ         = Output(Bool())
  })
  io.isJ := instrs.isJOp(io.instruction)

}

package core.components

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import config.Instructions

class RedirectorTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "instruction matcher" in {
    test(new InstrMatchTester) { c =>
      val beq = "b_0001_0000_0000_0000_0000_0000_0000_0000".U
      c.io.instruction.poke(beq)
      c.io.isBranchTwoOprand.expect(1.B)
      c.io.isBranchOneOprand.expect(0.B)
      val bgez = "b_0000_0111_1110_0001_1111_1111_0000_1111".U
      c.io.instruction.poke(bgez)
      c.io.isBranchTwoOprand.expect(0.B)
      c.io.isBranchOneOprand.expect(1.B)
    }
  }
}

package flute.core.backend.mdu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import flute.core.backend.decode.MDUOp

class MDUTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Divider"

  it should "get the right quotient and remainder" in {
    test(new MDU).withAnnotations(
      Seq(
        WriteVcdAnnotation, 
        VerilatorBackendAnnotation
      )
    ) { c =>
      c.io.op1.poke(12.U)
      c.io.op2.poke(3.U)
      c.io.md.poke(1.B)  // 0:div 1:mul
      c.io.signed.poke(0.B)

      c.clock.step(40)
      c.io.result.hi.bits.expect(0.U)
      c.io.result.lo.bits.expect(36.U)
    }
  }
}


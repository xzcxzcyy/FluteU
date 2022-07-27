package flute.core.backend.mdu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MDUTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Divider"

  it should "get the right quotient and remainder" in {
    test(new Div).withAnnotations(
      Seq(
        WriteVcdAnnotation, 
        VerilatorBackendAnnotation
      )
    ) { c =>
      c.io.x.poke(128.U)
      c.io.y.poke(3.U)
      c.io.signed.poke(0.B)

      c.clock.step(32)
      c.io.result.hi.bits.expect(2.U)
      c.io.result.lo.bits.expect(42.U)
    }
  }
}


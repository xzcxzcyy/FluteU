package flute.core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BetaTopTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  it should "look" in {
    test(new BetaTop("test_data/xor.in", "test_data/zero.in")).withAnnotations(Seq(WriteVcdAnnotation)) {c =>
      for(i <- 0 until 50) {
        println(c.io.pc.peek())
        c.clock.step()
      }
    }
  }
}
package flute.core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BetaTopTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  it should "look" in {
    test(new BetaTop("target/clang/branch.hexS", "test_data/zero.in")).withAnnotations(Seq(WriteVcdAnnotation)) {c =>
      for(i <- 0 until 64) {
        c.io.hwIntr.poke(0.U)
        println(c.io.pc.peek())
        c.clock.step()
      }
    }
  }
}
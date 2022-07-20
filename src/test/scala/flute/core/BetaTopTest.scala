package flute.core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sys.process._

class BetaTopTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  it should "look" in {
    test(new BetaTop("target/clang/branch.hexS", "test_data/zero.in"))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        )
      ) { c =>
        for (i <- 0 until 400) {
          c.io.hwIntr.poke(0.U)
          println(c.io.pc.peek())
          c.clock.step()
        }
        "sed -i -e 1,2d test_run_dir/should_look/BetaTop.vcd".!
      }
  }

  // it should "add" in {
  //   test(new BetaTop("target/clang/add.hexS", "test_data/zero.in"))
  //     .withAnnotations(
  //       Seq(
  //         WriteVcdAnnotation,
  //         VerilatorBackendAnnotation,
  //       )
  //     ) { c =>
  //       for (i <- 0 until 32) {
  //         c.io.hwIntr.poke(0.U)
  //         println(c.io.pc.peek())
  //         c.clock.step()
  //       }
  //       "sed -i -e 1,2d test_run_dir/should_add/BetaTop.vcd".!
  //     }
  // }
}

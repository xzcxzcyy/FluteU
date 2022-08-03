package flute.core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sys.process._

class BetaBaseTest(
    imemFile: String,
    dmemFile: String,
    shouldName: String,
    cycles: Int,
) extends AnyFlatSpec
    with ChiselScalatestTester
    with Matchers {
  it should shouldName in {
    test(new BetaTop(imemFile, dmemFile))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        )
      ) { c =>
        c.clock.setTimeout(0)
        for (i <- 0 until cycles) {
          c.io.hwIntr.poke(0.U)
          println(c.io.pc.peek())
          c.clock.step()
        }
        s"sed -i -e 1,2d test_run_dir/should_${shouldName}/BetaTop.vcd".!
      }
  }
}

class BetaBranchTest
    extends BetaBaseTest(
      "target/clang/branch.hexS",
      "test_data/zero.in",
      "branch",
      500,
    )

class BetaAddTest
    extends BetaBaseTest(
      "target/clang/add.hexS",
      "test_data/zero.in",
      "add",
      32,
    )

class BetaSortTest
    extends BetaBaseTest(
      "target/clang/sort.hexS",
      "test_data/sort.dcache",
      "sort",
      6000,
    )

class BetaGeneralTest
    extends BetaBaseTest(
      "target/clang/displayLight.hexS",
      "test_data/zero.in",
      "general",
      1024,
    )

class BetaDisplayLightTest
    extends BetaBaseTest(
      "target/clang/displayLight.hexS",
      "test_data/zero.in",
      "display_light",
      1024,
    )

class BetaMultTest
    extends BetaBaseTest(
      "target/clang/mul.hexS",
      "test_data/zero.in",
      "mult_div",
      1024,
    )
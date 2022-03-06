package flute.core

import chisel3._
import treadle.TreadleTester

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.ChiselScalatestTester

import firrtl.stage.FirrtlSourceAnnotation
import firrtl.options.TargetDirAnnotation

import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

class CoreTreadleTester extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "s1_base" in {
    val firrtlAnno = (new ChiselStage).execute(
        Array(),
        Seq(
            TargetDirAnnotation("target"),
            ChiselGeneratorAnnotation(() => new CoreTester("s1_base"))
        )
    )

    val tester = TreadleTester(firrtlAnno)
    tester.step()
    tester.expect("core.fetch.pc.io_out", 32)

    tester.report()
  }
}
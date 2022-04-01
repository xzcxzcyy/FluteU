package flute.cp0

import chisel3._
import treadle.TreadleTester

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.ChiselScalatestTester

import firrtl.stage.FirrtlSourceAnnotation
import firrtl.options.TargetDirAnnotation
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

import java.io.PrintWriter
import java.io.File
import flute.util.BaseTestHelper

class TestHelper(logName: String) extends BaseTestHelper(logName) {

  override val firrtlAnno = (new ChiselStage).execute(
    Array(),
    Seq(
      TargetDirAnnotation("target"),
      ChiselGeneratorAnnotation(() => new CP0)
    )
  )

  def printCP0Regs = {
    writer.println(s"BadVAddr: ")
  }
}

class CP0Test extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "general test" in {

  }
}

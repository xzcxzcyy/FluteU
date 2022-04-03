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

  override val t = TreadleTester(firrtlAnno)

  def printCP0Regs() = {
    val badvaddr = t.peek("io_debug_badvaddr")
    val count    = t.peek("io_debug_count")
    val status   = t.peek("io_debug_status")
    val cause    = t.peek("io_debug_cause")
    val epc      = t.peek("io_debug_epc")
    val compare  = t.peek("io_debug_compare")
    fprintln(s"BadVAddr: ${"0x%08x".format(badvaddr)}")
    fprintln(s"Count: ${"0x%08x".format(count)}")
    fprintln(s"Status: ${"0x%08x".format(status)}")
    fprintln(s"Cause: ${"0x%08x".format(cause)}")
    fprintln(s"Epc: ${"0x%08x".format(epc)}")
    fprintln(s"Compare: ${"0x%08x".format(compare)}")
  }
}

class CP0Test extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "general test" in {
    val t = new TestHelper(logName = "cp0general") 
    for (i <- 0 until 64) yield {
      t.step()
      t.printCP0Regs()
    }
    t.close()
  }
}

package flute.core.backend.lsu

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

import chisel3._
import chisel3.util._

class Tester(logName: String) extends BaseTestHelper(logName, () => new LSU) {}

class LSUTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "empty" in {
    val tester = new Tester("lsu_empty")
    tester.step(1)
    tester.close()
  }
}

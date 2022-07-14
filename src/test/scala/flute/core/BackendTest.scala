package flute.core

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

private class BackendTestHelper(fileName: String) extends BaseTestHelper(fileName, () => new Backend(nWays = 2)) {

}

class BackendTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "compile" in {
    val t = new BackendTestHelper(s"Backend Test")
    t.writer.println()
    t.writer.close()
  }

}
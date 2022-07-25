package flute.core.backend.rename

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

private class RMTTestHelper(fileName: String)
    extends BaseTestHelper(fileName, () => new RMT(2, 2, false)) {

    
}

class RMTTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "compile" in {
    val test = new RMTTestHelper("RMTTset")
    test.writer.println("empty")
  }
}

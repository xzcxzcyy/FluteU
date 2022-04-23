package flute.core.components

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

private class TestHelper(logName: String) extends BaseTestHelper(logName, () => new Sbuffer(32)) {
}

class SbufferTest extends AnyFreeSpec with Matchers {
  "empty test" in {
    val testHelper = new TestHelper("sbuffer_empty")
    testHelper.close()
  }
}

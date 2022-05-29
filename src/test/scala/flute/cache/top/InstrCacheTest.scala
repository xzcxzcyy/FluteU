package flute.cache.top

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.config.CPUConfig._

private class TestHelper(logName: String) extends BaseTestHelper(logName, () => new InstrCache(iCacheConfig)) {
}

class InstrCacheTest extends AnyFreeSpec with Matchers {
  "empty test" in {
    val testHelper = new TestHelper("instr_cache_empty")
    testHelper.close()
  }
}
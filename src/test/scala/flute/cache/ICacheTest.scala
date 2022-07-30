package flute.cache

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.util.BitMode.fromIntToBitModeLong
import flute.config.CPUConfig._

class ICacheTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new ICache("test_data/fetch_icache_test.in")) { c => }
  }
}

class ICacheAXITest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "compile" in {
    test(new ICacheWithAXI(iCacheConfig)) { c => }
  }
}

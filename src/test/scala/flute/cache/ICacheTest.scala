package flute.cache

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.util.BitMode.fromIntToBitModeLong
import flute.config.CPUConfig._

class ICacheTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  
}

class ICacheAXITest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "compile" in {
    test(new ICacheWithAXI(iCacheConfig)) { c => }
  }
}

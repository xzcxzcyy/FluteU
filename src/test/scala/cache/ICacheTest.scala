package cache

import chisel3._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import fluteutil.BitMode.fromIntToBitModeLong
import config.CpuConfig._
import cache.ICache

class ICacheTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new ICache) { c => 
      c.io.addr.bits.poke(4.U)
      c.io.addr.valid.poke(1.B)

      c.clock.step(5)

      c.io.data.valid.expect(1.B)

      printf(p"${c.io.data.bits}")
    }
  }
}

package cache

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import fluteutil.BitMode.fromIntToBitModeLong
import config.CpuConfig._

class ICacheTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new ICache) { c =>
      c.io.addr.bits.poke(4.U)
      c.io.addr.valid.poke(1.B)

      c.clock.step(5)

      c.io.data.valid.expect(1.B)

      c.io.data.bits(0).expect(0x380200ff.BM.U)
      for (i <- 1 to 7) {
        if (i == 4) {
          c.io.data.bits(i).expect(0x00221826.BM.U)
        } else {
          c.io.data.bits(i).expect(0.U)
        }
      }

      c.io.addr.valid.poke(0.B)
      c.clock.step()
      c.io.data.valid.expect(1.B)
      c.clock.step()
      c.io.data.valid.expect(0.B)


      c.io.addr.bits.poke(4.U)
      c.io.addr.valid.poke(1.B)

      c.clock.step(5)

      c.io.data.valid.expect(1.B)


      c.io.addr.bits.poke(0.U)
      c.clock.step()
      c.io.data.valid.expect(1.B)
      c.io.data.bits(0).expect(0x380200ff.BM.U)
      c.clock.step()
      c.io.data.valid.expect(1.B)
      c.io.data.bits(0).expect(0x3801ffff.BM.U)

    }
  }
}

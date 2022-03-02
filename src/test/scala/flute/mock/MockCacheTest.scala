package flute.mock

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import flute.util.BitMode.fromIntToBitModeLong
import flute.config.CPUConfig._
import flute.core.decode.StoreMode

class MockCacheTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  val content = Seq(
    1L,
    2L,
    4L,
    8L,
  )
  "mock cache test" in {
    test(new MockCache(content)) { cache =>
      val step = () => cache.clock.step()

      // cache.reset.poke(1.B)
      // step()
      // cache.reset.poke(0.B)

      cache.io.readAddr.poke(12.U)
      cache.io.readData.expect(8.U)
      cache.io.writeAddr.poke(12.U)
      cache.io.storeMode.poke(StoreMode.word)
      cache.io.writeData.poke(6.U)
      step()
      cache.io.readAddr.poke(12.U)
      cache.io.readData.expect(6.U)
    }
  }
}

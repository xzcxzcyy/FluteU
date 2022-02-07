package mock

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import fluteutil.BitMode.fromIntToBitModeLong
import config.CpuConfig._

class MockInstrMemTest extends AnyFreeSpec with ChiselScalatestTester with Matchers{
  "Rom test" in {
      test(new MockInstrMem("./test_data/mem.in")) { c =>
        for(i <- 0 to 8) {
            c.io.addr.poke(i.U)
            c.clock.step()
        }
    }
  }
}

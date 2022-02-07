package mock

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import fluteutil.BitMode.fromIntToBitModeLong
import config.CpuConfig._

class MockInstrMemTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Rom test" in {
    test(new MockInstrMem("./test_data/mem.in")) { c =>
      val step = () => c.clock.step()
      var pc = 0.U
      c.io.addr.poke(pc)
      c.io.ready.expect(true.B)
      c.io.dataOut.expect(0x55.U)

      c.io.addr.poke(4.U)
      c.io.ready.expect(false.B)

      step()
      c.io.ready.expect(true.B)
      c.io.dataOut.expect(0xe5.U)

      step()
      step()

      c.io.addr.poke(12.U)
      c.io.ready.expect(false.B)

      step()

      c.io.ready.expect(true.B)
      c.io.dataOut.expect(0x4.U)

    // c.io.dataOut.expect(0x55.U)
    }
  }
}

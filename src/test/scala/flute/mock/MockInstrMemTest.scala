package flute.mock

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.util.BitMode.fromIntToBitModeLong
import flute.config.CPUConfig._

class MockInstrMemTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Rom test" in {
    test(new MockInstrMem("./test_data/mem.in")) { c =>
      val step = () => c.clock.step()

      c.io.addr.poke(0.U)
      c.io.valid.expect(true.B)
      c.io.dataOut.expect(0x55.U)

      c.io.addr.poke(4.U)
      c.io.valid.expect(false.B)

      step()
      c.io.valid.expect(true.B)
      c.io.dataOut.expect(0xe5.U)

      step()
      step()

      c.io.addr.poke(12.U)
      c.io.valid.expect(false.B)

      step()

      c.io.valid.expect(true.B)
      c.io.dataOut.expect(0x4.U)

    // c.io.dataOut.expect(0x55.U)
    }
  }
}

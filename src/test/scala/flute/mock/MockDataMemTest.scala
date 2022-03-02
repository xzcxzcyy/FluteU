package flute.mock

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


class MockDataMemTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {

  "R-W-R Test" in {
    test(new MockDataMem("./test_data/mem.in")) { c =>
      def step(n: Int = 1) = {
        c.clock.step(n)
      }

      c.io.enable.poke(true.B)

      // read
      c.io.write.poke(false.B)
      c.io.addr.poke(4.U)

      c.io.valid.expect(false.B)
      step()
      c.io.valid.expect(true.B)
      c.io.dataOut.expect(0xe5.U)

      // write
      c.io.write.poke(true.B)
      c.io.dataIn.poke(0x112.U)
      c.io.addr.poke(4.U) 
      // now valid can be no matter what, we do not care
    
      step()
      c.io.valid.expect(false.B)
      
      // read
      c.io.write.poke(false.B)
      c.io.addr.poke(4.U)

      step()
      c.io.valid.expect(true.B)
      c.io.dataOut.expect(0x112.U)

    }
  }
  "Read-only Test" in {
    test(new MockDataMem("./test_data/mem.in")) { c =>
      val step = () => c.clock.step()

      c.io.enable.poke(true.B)
      c.io.write.poke(false.B)

      c.io.valid.expect(true.B)
      c.io.dataOut.expect(0x55.U)

      c.io.addr.poke(4.U)
      c.io.valid.expect(false.B)

      step()

      c.io.valid.expect(true.B)
      c.io.dataOut.expect(0xe5.U)
    }
  }
}

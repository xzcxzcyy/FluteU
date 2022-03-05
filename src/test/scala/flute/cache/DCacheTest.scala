package flute.cache

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.util.BitMode.fromIntToBitModeLong
import flute.config.CPUConfig._
import flute.core.decode.StoreMode

class DCacheTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test multi read port" in {
    test(new DCache("test_data/dmem.in")) { c =>
      c.io.port(0).addr.poke(0.U)
      c.io.port(0).storeMode.poke(StoreMode.disable)
      c.io.port(1).addr.poke(4.U)
      c.io.port(1).storeMode.poke(StoreMode.disable)

      c.io.port(0).readData.expect(0.U)
      c.io.port(1).readData.expect(1.U)
    }
  }

  "test writeport" in {
    test(new DCache("test_data/dmem.in")) { c =>
      def writetest(p: Int, addr: Int, expectdata: Int, storemode: UInt) = {
        c.io.port(p).addr.poke(addr.U)
        c.io.port(p).storeMode.poke(storemode)
        c.io.port(p).writeData.poke(0x3f3f3f3f.U)

        c.clock.step()

        c.io.port(p).addr.poke(addr.U)
        c.io.port(p).readData.expect(expectdata.U)
      }

      writetest(1, 4, 0x00003f3f, StoreMode.halfword)
      writetest(1, 8, 0x0000003f, StoreMode.byte)
      writetest(0, 0, 0x3f3f3f3f, StoreMode.word)
    }
  }
  "test read first" in {
    test(new DCache("test_data/dmem.in")) { c =>
      c.io.port(0).writeData.poke(0x3f3f3f3f.U)
      c.io.port(0).addr.poke(12.U)
      c.io.port(0).storeMode.poke(StoreMode.word)
      c.io.port(0).addr.poke(12.U)

      c.io.port(0).readData.expect(3.U)
    }
  }
}

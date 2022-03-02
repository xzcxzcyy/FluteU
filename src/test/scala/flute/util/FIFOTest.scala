package flute.util

import chisel3._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.util.BitMode.fromIntToBitModeLong
import flute.config.CPUConfig._

class FIFOTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  /**
    * Should be further tested.
    */
  "general tests" in {
    test(new FIFOQueue(UInt(32.W), 1024, 2, 2)) { fifo =>
      // fifo.io.test.expect(0.U(32.W))
      for (i <- 0 until 2) {
        fifo.io.write(i).bits.poke(i.U)
        fifo.io.write(i).valid.poke(1.B)
        fifo.io.read(i).valid.expect(0.B)
      }
      fifo.clock.step()
      for (i <- 0 until 2) {
        fifo.io.read(i).ready.poke(1.B)
        fifo.io.write(i).ready.expect(1.B)
        fifo.io.read(i).valid.expect(1.B)
        fifo.io.read(i).bits.expect(i.U)
      }
    }
  }
}

package flute.util

import chisel3._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import fluteutil.BitMode.fromIntToBitModeLong
import config.CpuConfig._

class FIFOTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "fifo test" in {
    test(new FIFOQueue(UInt(32.W), 1024, 2, 2)) { fifo =>
      for (i <- 0 until 2) {
        fifo.io.write(i).bits.poke(i.U)
        fifo.io.write(i).valid.poke(1.B)
        fifo.io.read(i).ready.poke(1.B)
      }
      fifo.clock.step()
      for (i <- 0 until 2) {
        fifo.io.write(i).ready.expect(1.B)
      }
    }
  }
}

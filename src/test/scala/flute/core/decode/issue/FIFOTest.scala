package flute.core.decode.issue

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
    test(new FIFOQueue(UInt(32.W), 8, 2, 2)) { fifo =>
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

  "R/V test:through" in {
    // 队列允许15个元素
    test(new FIFOQueue(UInt(32.W), 8, 2, 2)) { fifo =>
      // 后续元件一直ready
      for (i <- 0 until 2) fifo.io.read(i).ready.poke(1.B)

      // 空队列时RV检查 ///////////////////////
      for (i <- 0 until 2) {
        fifo.io.write(i).ready.expect(1.B)
        fifo.io.read(i).valid.expect(0.B)
      }
      // 空队列时RV检查 ///////////////////////

      // 投喂一个 0x1f ////////////////////////
      fifo.io.write(0).bits.poke(0x1f.U)
      fifo.io.write(0).valid.poke(1.B)
      fifo.io.write(1).valid.poke(0.B)
      // 投喂一个0x1f ////////////////////////
      fifo.clock.step()

      // RV检查 /////////////////////////////
      for (i <- 0 until 2) {
        fifo.io.write(i).ready.expect(1.B)
        fifo.io.read(i).valid.expect((i == 0).B)
      }
      // RV检查 /////////////////////////////

      // Data检查 ///////////////////////////
      fifo.io.read(0).bits.expect(0x1f.U)
      // Data检查 ///////////////////////////

      // 投喂两个 0x2f 和 0x1f /////////////////
      fifo.io.write(0).bits.poke(0x2f.U)
      fifo.io.write(0).valid.poke(1.B)
      fifo.io.write(1).bits.poke(0x3f.U)
      fifo.io.write(1).valid.poke(1.B)
      // 投喂两个 0x2f 和 0x1f /////////////////
      fifo.clock.step()

      // RV检查 /////////////////////////////
      for (i <- 0 until 2) {
        fifo.io.write(i).ready.expect(1.B)
        fifo.io.read(i).valid.expect(1.B)
      }
      // RV检查 /////////////////////////////

      // Data检查 ///////////////////////////
      fifo.io.read(0).bits.expect(0x2f.U)
      fifo.io.read(1).bits.expect(0x3f.U)
      // Data检查 ///////////////////////////

    }
  }

  "R/V test:full" in {
    test(new FIFOQueue(UInt(32.W), 8, 2, 2)) { fifo =>
      // 后续元件一直阻塞
      for (i <- 0 until 2) fifo.io.read(i).ready.poke(0.B)

      // 填入6个元素
      for (j <- 0 until 3) {
        for (i <- 0 until 2) {
          fifo.io.write(i).bits.poke((i + 1).U)
          fifo.io.write(i).valid.poke(1.B)
        }
        fifo.clock.step()
      }
      // RV检查 /////////////////////////////
      for (i <- 0 until 2) {
        fifo.io.write(i).ready.expect((i == 0).B)
        fifo.io.read(i).valid.expect(1.B)
      }
      // RV检查 /////////////////////////////

      // 填入两个元素
      for (i <- 0 until 2) {
        fifo.io.write(i).bits.poke((i + 1).U)
        fifo.io.write(i).valid.poke(1.B)
      }
      fifo.clock.step()
      // 实际进入1个

      // RV检查 /////////////////////////////
      for (i <- 0 until 2) {
        fifo.io.write(i).ready.expect(0.B)
        fifo.io.read(i).valid.expect(1.B)
      }
      // RV检查 /////////////////////////////

    }
  }
}

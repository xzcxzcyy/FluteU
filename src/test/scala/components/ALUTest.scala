package components

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import components.ALUOp

import config.CpuConfig._
import scala.util.Random

class ALUTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "ALU: add" in {
    test(new ALU) { alu =>
      val io     = alu.io
      val rander = new Random()
      val overflowTest = (isUnsigned: Boolean) => {
        val INT_MAX = (1L << 31) - 1
        val CMPL = (1L << 32)
        for (i <- 1 to 10) {
          val sum = rander.nextInt(INT_MAX.toInt - 1).toLong + INT_MAX + 1
          val x = rander.nextInt((INT_MAX * 2 - sum).toInt).toLong + (sum - INT_MAX)
          val y = (sum - x).toLong
          io.x.poke(x.U(dataWidth.W))
          io.y.poke(y.U(dataWidth.W))
          io.result.expect((x + y).U)
          io.flag.trap.expect((!isUnsigned).B)
          val negx = CMPL - x
          val negy = CMPL - y
          val negSum = CMPL - sum
          io.x.poke(negx.U)
          io.y.poke(negy.U)
          io.result.expect(negSum.U)
          io.flag.trap.expect((!isUnsigned).B)
        }
      }
      io.aluOp.poke(ALUOp.add)
      overflowTest(false)
      io.aluOp.poke(ALUOp.addu)
      overflowTest(true)
    }
  }
}

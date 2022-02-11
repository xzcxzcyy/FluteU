package core.components

import fluteutil.BitMode.fromIntToBitModeLong
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import config.CpuConfig._
import core.components.ALU

import scala.util.Random

class ALUTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "add/addu overflow tests" in {
    test(new ALU) { alu =>
      val io     = alu.io
      val rander = new Random()
      val overflowTest = (isUnsigned: Boolean) => {
        val INT_MAX = (1L << 31) - 1
        val CMPL    = (1L << 32)
        for (i <- 1 to 10) {
          val sum = rander.nextInt(INT_MAX.toInt - 1).toLong + INT_MAX + 1
          val x   = rander.nextInt((INT_MAX * 2 - sum).toInt).toLong + (sum - INT_MAX)
          val y   = (sum - x).toLong
          io.x.poke(x.U(dataWidth.W))
          io.y.poke(y.U(dataWidth.W))
          io.result.expect((x + y).U)
          io.flag.trap.expect((!isUnsigned).B)
          val negx   = CMPL - x
          val negy   = CMPL - y
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

  "add/addu overflow-less test" in {
    test(new ALU) { alu =>
      val testFn = (x: Int, y: Int) => {
        alu.io.aluOp.poke(ALUOp.add)
        alu.io.x.poke(x.BM.U)
        alu.io.y.poke(y.BM.U)
        alu.io.flag.equal.expect((x == y).B)
        alu.io.flag.trap.expect(0.B)
        alu.io.result.expect((x + y).BM.U)
        ///
        alu.io.aluOp.poke(ALUOp.addu)
        alu.io.flag.equal.expect((x == y).B)
        alu.io.flag.trap.expect(0.B)
        alu.io.result.expect((x + y).BM.U)
      }
      val cases = Seq(
        (-12432, 123),
        (-2147483647, -1),
        (2147483646, 1),
        (-12432, 20000),
        (120, -12000),
        (2000, -1240),
        (100000, 1000001),
        (-100000, -1000001),
        (100001, 100001),
      )
      cases.foreach((c) => testFn(c._1, c._2))
    }
  }

  "sub/subu tests" in {
    test(new ALU) { alu =>
      val testFn = (x: Int, y: Int, overflow: Boolean) => {
        val io = alu.io
        io.aluOp.poke(ALUOp.sub)
        io.x.poke(x.BM.U)
        io.y.poke(y.BM.U)
        io.flag.equal.expect((x == y).B)
        io.result.expect((x - y).BM.U)
        io.flag.trap.expect(overflow.B)
        ///
        io.aluOp.poke(ALUOp.subu)
        io.flag.equal.expect((x == y).B)
        io.result.expect((x - y).BM.U)
        io.flag.trap.expect(0.B)
      }
      val overflowTestCases = Seq(
        (-2147483647, 200),
        (-2147483648, 1),
        (1, -2147483648),
        (100000, -2147383748),
        (2147483647, -1),
      )
      overflowTestCases.foreach((c) => testFn(c._1, c._2, true))
      val nonOverflowTestCases = Seq(
        (-2147483647, 1),
        (-1, 2147483647),
        (2147483647, 0),
        (2147483646, -1),
        (120, -12000),
        (2000, -1240),
        (100000, 1000001),
        (-100000, -1000001),
        (100001, 100001),
      )
      nonOverflowTestCases.foreach((c) => testFn(c._1, c._2, false))
    }
  }

  "bitwise and conditional set instructions" in {
    test(new ALU) { c =>
      val io   = c.io
      val rand = new Random()
      def doTest(aluop: UInt, ansFn: (Long, Long) => Long, epoches: Int = 10): Unit = {
        for (i <- 0 to epoches) {
          val x   = rand.nextLong(1L << 32)
          val y   = rand.nextLong(1L << 32)
          val res = ansFn(x, y)
          io.aluOp.poke(aluop)
          io.x.poke(x.U)
          io.y.poke(y.U)
          io.result.expect(res.U)
        }
      }
      // AND
      doTest(ALUOp.and, (op1, op2) => (op1 & op2))
      // OR
      doTest(ALUOp.or, (op1, op2) => (op1 | op2))
      // NOR
      doTest(ALUOp.nor, (op1, op2) => ((op1 | op2) ^ (-1).BM))
      // XOR
      doTest(ALUOp.xor, (op1, op2) => (op1 ^ op2))
      // SLTU
      doTest(ALUOp.sltu, (op1, op2) => if (op1 < op2) 1L else 0L)
      // SLT
      def testSLT(x: Int, y: Int): Unit = {
        io.aluOp.poke(ALUOp.slt)
        io.x.poke(x.BM.U)
        io.y.poke(y.BM.U)
        val answer = if (x < y) 1.U(dataWidth.W) else 0.U(dataWidth.W)
        io.result.expect(answer)
      }
      val sltCases = Seq(
        (1, 2),
        (2, 1),
        (1, 1),
        (2147483647, -2147483648),
        (2147483647, -1),
        (-2147483648, 2147483647),
      )
      sltCases.foreach(c => testSLT(c._1, c._2))
    }
  }

  "instruction specified shamt shift operations" in {
    test(new ALU) { alu =>
      val io = alu.io
      def doTest(x: Int, y: Int, aluop: UInt, ansFn: (Long, Long) => Long): Unit = {
        io.aluOp.poke(aluop)
        io.x.poke(x.BM.U)
        io.y.poke(y.BM.U)
        io.result.expect(ansFn(x.BM, y.BM).U)
      }
      def shamt(num: Long) = num & 0x1fL
      def sllAnsFn(x: Long, y: Long): Long = {
        (y << shamt(x)) & 0xffffffffL
      }
      def srlAnsFn(x: Long, y: Long): Long = {
        (y >> shamt(x)) & 0xffffffffL
      }
      def sraAnsFn(x: Long, y: Long): Long = {
        ((y.toInt) >> shamt(x).toInt).BM
      }
      val cases = Seq(
        (5, 101),
        (0, 23),
        (30, -1),
        (30, -2),
        (123, 102),
        (123, 24),
        (1111111, -2),
        (2222222, -3),
      )
      cases.foreach(t => doTest(t._1, t._2, ALUOp.sll, sllAnsFn))
      cases.foreach(t => doTest(t._1, t._2, ALUOp.srl, srlAnsFn))
      cases.foreach(t => doTest(t._1, t._2, ALUOp.sra, sraAnsFn))
    }
  }

  "flag test" in {
    test(new ALU) { alu =>
      val io = alu.io
      val testFlagFn = (x: Int, y: Int) => {
        io.x.poke(x.BM.U)
        io.y.poke(y.BM.U)
        io.flag.equal.expect((x == y).asBool)
        io.flag.lessS.expect((x < y).asBool)
        io.flag.lessU.expect((x.BM < y.BM).asBool)
      }
      val cases = Seq(
        (1, 2),
        (2, 1),
        (-2, -3),
        (-3, -2),
        (2147483647, -1),
        (-1, 2147483647),
        (0, -2147483648),
        (-2147483648, 0),
        (1, 1),
        (-2147483648, -2147483648),
      )
      cases.foreach(c => testFlagFn(c._1, c._2))
    }
  }
}

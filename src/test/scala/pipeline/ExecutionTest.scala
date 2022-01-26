package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import PokeExpect.{Poke, Expect}
import components.ALUOp

class ExecutionTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Execution combination logic test" in {
    test(new Execution()) { c =>
      val from = c.io.fromId
      val to   = c.io.toMem
      // addu
      c.poke(true, false, false, ALUOp.addu, false, false, 101.U, 202.U, 1.U, 20.U, 0.U)
      c.expect(true, false, false, 303.U, 202.U, 1.U)
    }
  }
}

private object PokeExpect {
  implicit class Poke(c: Execution) {
    def poke(
        regWriteEn: Boolean = false,
        memToReg: Boolean = false,
        memWrite: Boolean = false,
        aluOp: UInt = 0.U,
        aluXFromShamt: Boolean = false,
        aluYFromImm: Boolean = false,
        rs: UInt = 0.U,
        rt: UInt = 0.U,
        writeRegAddr: UInt = 0.U,
        immediate: UInt = 0.U,
        shamt: UInt = 0.U,
    ): Unit = {
      val b = c.io.fromId
      b.control.regWriteEn.poke(regWriteEn.B)
      b.control.memToReg.poke(memToReg.B)
      b.control.memWrite.poke(memWrite.B)
      b.control.aluOp.poke(aluOp)
      b.control.aluXFromShamt.poke(aluXFromShamt.B)
      b.control.aluYFromImm.poke(aluYFromImm.B)
      b.rs.poke(rs)
      b.rt.poke(rt)
      b.writeRegAddr.poke(writeRegAddr)
      b.immediate.poke(immediate)
      b.shamt.poke(shamt)
    }
  }

  implicit class Expect(c: Execution) {
    def expect(
        regWriteEn: Boolean = false,
        memToReg: Boolean = false,
        memWrite: Boolean = false,
        aluResult: UInt = 0.U,
        memWriteData: UInt = 0.U,
        writeRegAddr: UInt = 0.U,
    ) = {
      val m = c.io.toMem
      m.control.regWriteEn.expect(regWriteEn.B)
      m.control.memToReg.expect(memToReg.B)
      m.control.memWrite.expect(memWrite.B)
      m.aluResult.expect(aluResult)
      m.memWriteData.expect(memWriteData)
      m.writeRegAddr.expect(writeRegAddr)
    }
  }
}

package core.pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import PokeExpect.{Expect, Poke}
import config.CpuConfig._
import core.pipeline.Execution
import fluteutil.BitMode.fromIntToBitModeLong

class ExecutionTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Arithmetic test" in {
    test(new Execution()) { c =>
      val from = c.io.fromId
      val to   = c.io.toMem
      // addu
      c.poke(true, false, false, ALUOp.addu, false, false, 101.U, 202.U, 1.U, 20.U, 0.U)
      c.expect(true, false, false, 303.U, 202.U, 1.U)

      // add
      c.poke(true, false, false, ALUOp.add, false, false, 101.U, 202.U, 1.U, 20.U, 0.U)
      c.expect(true, false, false, 303.U, 202.U, 1.U)

      // subu
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.subu,
        rs = 333.U,
        rt = 222.U,
        writeRegAddr = 2.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = 111.U,
        memWriteData = 222.U,
        writeRegAddr = 2.U,
      )

      // sub
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.sub,
        rs = 333.U,
        rt = 222.U,
        writeRegAddr = 2.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = 111.U,
        memWriteData = 222.U,
        writeRegAddr = 2.U,
      )

      // and
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.and,
        rs = -2.BM.U,
        rt = 3.U,
        writeRegAddr = 10.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = (-2.BM & 3L).U,
        memWriteData = 3.U,
        writeRegAddr = 10.U,
      )

      // or
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.or,
        rs = -2.BM.U,
        rt = 3.U,
        writeRegAddr = 10.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = (-2.BM | 3L).U,
        memWriteData = 3.U,
        writeRegAddr = 10.U,
      )

      // xor
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.xor,
        rs = -2.BM.U,
        rt = 3.U,
        writeRegAddr = 10.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = (-2.BM ^ 3L).U,
        memWriteData = 3.U,
        writeRegAddr = 10.U,
      )

      // nor
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.nor,
        rs = -2.BM.U,
        rt = 3.U,
        writeRegAddr = 10.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = ((-2.BM | 3L) ^ -1.BM).U,
        memWriteData = 3.U,
        writeRegAddr = 10.U,
      )

      // addiu
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.addu,
        aluYFromImm = true,
        rs = 9999.U,
        rt = 0.U,
        writeRegAddr = 5.U,
        immediate = -1.BM.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = 9998.U,
        memWriteData = 0.U,
        writeRegAddr = 5.U,
      )

      // sra
      c.poke(
        regWriteEn = true,
        aluOp = ALUOp.sra,
        aluXFromShamt = true,
        rt = -5.BM.U,
        writeRegAddr = 1.U,
        shamt = 2.U,
      )
      c.expect(
        regWriteEn = true,
        aluResult = -2.BM.U,
        memWriteData = -5.BM.U,
        writeRegAddr = 1.U,
      )

      // empty
      c.poke()
      c.expect()
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
      b.control.storeMode.poke(memWrite.B)
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
      m.control.storeMode.expect(memWrite.B)
      m.aluResult.expect(aluResult)
      m.memWriteData.expect(memWriteData)
      m.writeRegAddr.expect(writeRegAddr)
    }
  }
}

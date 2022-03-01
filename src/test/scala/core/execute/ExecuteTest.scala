package core.execute

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import config.CPUConfig._
import core.execute.aluexec.ALUExecutor
import core.components.ALUOp

class ExecuteTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new ALUExecutor) { c =>
      c.io.source.bits.controlSig.aluOp.poke(ALUOp.xor)
      c.io.source.bits.controlSig.regWriteEn.poke(1.B)
      c.io.source.bits.writeRegAddr.poke(1.U)
      c.io.source.bits.controlSig.aluYFromImm.poke(1.B)
      c.io.source.bits.immediate.poke(0xffff.U)
      c.io.source.valid.poke(1.B)
      
      c.clock.step()

      c.io.source.valid.poke(0.B)

      c.clock.step(2)

      c.io.regFile.writeEnable.expect(1.B)
      c.io.regFile.writeData.expect(0xffff.U)
      c.io.regFile.writeAddr.expect(1.U)

    }
  }

  "test2" in {
    test(new Execute) { c =>
      c.io.withDecode.microOps(0).bits.controlSig.aluOp.poke(ALUOp.xor)
      c.io.withDecode.microOps(0).bits.controlSig.regWriteEn.poke(1.B)
      c.io.withDecode.microOps(0).bits.writeRegAddr.poke(1.U)
      c.io.withDecode.microOps(0).bits.controlSig.aluYFromImm.poke(1.B)
      c.io.withDecode.microOps(0).bits.immediate.poke(0xffff.U)
      c.io.withDecode.microOps(0).valid.poke(1.B)

      c.io.withDecode.microOps(1).bits.controlSig.aluOp.poke(ALUOp.xor)
      c.io.withDecode.microOps(1).bits.controlSig.regWriteEn.poke(1.B)
      c.io.withDecode.microOps(1).bits.writeRegAddr.poke(2.U)
      c.io.withDecode.microOps(1).bits.controlSig.aluYFromImm.poke(1.B)
      c.io.withDecode.microOps(1).bits.immediate.poke(0x00ff.U)
      c.io.withDecode.microOps(1).valid.poke(1.B)
      
      c.clock.step()

      c.io.withDecode.microOps(0).valid.poke(0.B)

      c.io.withDecode.microOps(1).valid.poke(0.B)

      c.clock.step(2)

      c.io.withRegFile(0).writeEnable.expect(1.B)
      c.io.withRegFile(0).writeData.expect(0xffff.U)
      c.io.withRegFile(0).writeAddr.expect(1.U)

      c.io.withRegFile(1).writeEnable.expect(1.B)
      c.io.withRegFile(1).writeData.expect(0x00ff.U)
      c.io.withRegFile(1).writeAddr.expect(2.U)

    }
  }

}


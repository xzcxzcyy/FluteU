package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import config.CpuConfig._

class StageRegTests extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "IF/ID Stage Test" in {
    test(new IfIdStage()) { s =>
      s.io.flush.poke(0.B)
      s.io.valid.poke(1.B)
      s.io.in.instruction.poke(0x1bbbbbbb.U(32.W))
      s.io.in.pcplusfour.poke(8.U(32.W))
      s.clock.step()
      s.io.data.instruction.expect(0x1bbbbbbb.U(32.W))
      s.io.data.pcplusfour.expect(8.U(32.W))
      ////
      s.io.valid.poke(0.B)
      s.io.in.instruction.poke(0x3bbbbbbb.U(32.W))
      s.clock.step()
      s.io.data.instruction.expect(0x1bbbbbbb.U(32.W))
      ////
      s.io.flush.poke(1.B)
      s.clock.step()
      s.io.data.instruction.expect(0.U(32.W))
      s.io.data.pcplusfour.expect(0.U(32.W))
      ////
      s.io.flush.poke(1.B)
      s.io.valid.poke(1.B)
      s.io.in.instruction.poke(555.U(32.W))
      s.io.in.pcplusfour.poke(555.U(32.W))
      s.clock.step()
      s.io.data.instruction.expect(0.U(32.W))
      s.io.data.pcplusfour.expect(0.U(32.W))
    }
  }

  "ID/EX Stage Test" in {
    test(new IdExStage()) { s =>
      val data = s.io.data
      val io   = s.io
      io.flush.poke(1.B)
      s.clock.step()
      data.control.aluOp.expect(0.U(aluOpWidth.W))
      data.control.aluSrc.expect(0.B)
      data.immediate.expect(0.U)
      data.control.branchCond.expect(0.U)
      io.in.control.aluOp.poke(1.U)
      io.in.control.branchCond.poke(BranchCond.eq)
      io.flush.poke(0.B)
      io.valid.poke(1.B)
      io.in.rs.poke(28.U)
      s.clock.step()
      data.control.aluOp.expect(1.U)
      data.control.branchCond.expect(BranchCond.eq)
      data.rs.expect(28.U)
    }
  }

  "EX/MEM Stage Test" in {
    test(new ExMemStage()) { s =>
      val data = s.io.data
      val io   = s.io
      io.flush.poke(1.B)
      io.in.control.memToReg.poke(0.B)
      s.clock.step()
      ///
      data.control.memToReg.expect(0.B)
      data.writeData.expect(0.U)
      ///
      io.in.aluResult.poke(28.U)
      io.in.control.memWrite.poke(1.B)
      io.flush.poke(0.B)
      io.valid.poke(1.B)
      s.clock.step()
      ///
      data.control.memWrite.expect(1.B)
      data.aluResult.expect(28.U)
    }
  }

  "MEM/WB Stage Test" in {
    test(new MemWbStage()) { s =>
      val valid = s.io.valid
      val flush = s.io.flush
      val clock = s.clock
      val in    = s.io.in
      val out   = s.io.data

      // load
      valid.poke(1.B)
      flush.poke(0.B)
      in.control.memToReg.poke(1.B)
      in.aluResult.poke(233.U)
      in.dataFromMem.poke(234.U)
      clock.step()
      out.control.memToReg.expect(1.B)
      out.aluResult.expect(233.U)
      out.dataFromMem.expect(234.U)
      // flush
      flush.poke(1.B)
      clock.step()
      out.control.memToReg.expect(0.B)
      out.aluResult.expect(0.U)
      out.dataFromMem.expect(0.U)
      // load
      flush.poke(0.B)
      valid.poke(1.B)
      in.writeRegAddr.poke(31.U)
      clock.step()
      out.writeRegAddr.expect(31.U)
      // stall
      valid.poke(0.B)
      in.writeRegAddr.poke(30.U)
      clock.step()
      out.writeRegAddr.expect(31.U)
      // stall & flush
      flush.poke(1.B)
      clock.step()
      out.writeRegAddr.expect(0.U)
    }
  }
}

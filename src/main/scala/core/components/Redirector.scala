package core.components

import chisel3._
import config.CpuConfig._
import config.BasicInstructions
import chisel3.util.BitPat

private object instr extends BasicInstructions {
  private def combine(s: Seq[BitPat])(instruction: UInt) = {
    assert(instruction.getWidth == instrWidth)
    s.foldLeft(0.B)((res, bitpat) => res || (instruction === bitpat))
  }

  def isBranchTwoOprand = combine(
    Seq(
      BEQ,
      BNE,
    )
  ) _

  def isBranchOneOprand = combine(
    Seq(
      BGEZ,
      BGTZ,
      BLEZ,
      BLTZ,
      BGEZAL,
      BLTZAL,
      JR,
      JALR,
    )
  ) _
}

class InstrMatchTester extends Module {
  val io = IO(new Bundle {
    val instruction       = Input(UInt(instrWidth.W))
    val isBranchTwoOprand = Output(Bool())
    val isBranchOneOprand = Output(Bool())
  })
  io.isBranchOneOprand := instr.isBranchOneOprand(io.instruction)
  io.isBranchTwoOprand := instr.isBranchTwoOprand(io.instruction)
}

class Redirector extends Module {
  class IdIO extends Bundle {
    val instruction = Input(UInt(dataWidth.W))
    val idRsChoice  = Output(UInt(idRedirectChoiceWidth.W))
    val idRtChoice  = Output(UInt(idRedirectChoiceWidth.W))
    val rsFwd       = Output(UInt(redirectExFwdWidth.W))
    val rtFwd       = Output(UInt(redirectExFwdWidth.W))
  }

  class ExIO extends Bundle {
    val regAddr = Input(UInt(regAddrWidth.W))
  }

  class MemIO extends Bundle {
    val regAddr = Input(UInt(regAddrWidth.W))
  }

  class WbIO extends Bundle {
    val regAddr = Input(UInt(regAddrWidth.W))
  }

  val io = IO(new Bundle {
    val id        = new IdIO
    val ex        = new ExIO
    val mem       = new MemIO
    val wb        = new WbIO
    val ifIdStall = Output(Bool())
    val idExStall = Output(Bool())
    val idExFlush = Output(Bool())
  })
}

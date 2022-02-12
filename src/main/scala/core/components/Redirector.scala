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

object IdFwd {
  val width = 2

  val none    = 0.U(width.W)
  val fromMem = 2.U(width.W)
  val fromWb  = 3.U(width.W)
}

object ExFwd {
  val width = 2

  val none = 0.U(width.W)

  // distance
  val one   = 1.U(width.W)
  val two   = 2.U(width.W)
  val three = 3.U(width.W)
}

class Redirector extends Module {
  class IdIO extends Bundle {
    val instruction = Input(UInt(dataWidth.W))
    val idRsChoice  = Output(UInt(IdFwd.width.W))
    val idRtChoice  = Output(UInt(IdFwd.width.W))
    val rsFwd       = Output(UInt(ExFwd.width.W))
    val rtFwd       = Output(UInt(ExFwd.width.W))
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

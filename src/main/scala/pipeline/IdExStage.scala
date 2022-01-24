package pipeline

import chisel3._
import config.CpuConfig._

class IdExStage extends StageReg(new IdExBundle) {}

class IdExBundle extends Bundle {
  class ControlBundle extends Bundle {
    val regWriteEn    = Bool()
    val memToReg      = Bool()
    val memWrite      = Bool()
    val aluOp         = UInt(aluOpWidth.W)
    val branchCond    = UInt(branchCondWidth.W)
    val aluYFromImm   = Bool()
    val aluXFromShamt = Bool()
  }

  val control      = new ControlBundle()
  val rs           = UInt(dataWidth.W)
  val rt           = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
  val immediate    = UInt(dataWidth.W)
  val pcplusfour   = UInt(addrWidth.W)
  val shamt        = UInt(shamtWidth.W)
}

object BranchCond {
  val none = 0.U(branchCondWidth.W)
  val eq   = 1.U(branchCondWidth.W)
  val ge   = 2.U(branchCondWidth.W)
  val geu  = 3.U(branchCondWidth.W)
  val gt   = 4.U(branchCondWidth.W)
  val gtu  = 5.U(branchCondWidth.W)
  val le   = 6.U(branchCondWidth.W)
  val leu  = 7.U(branchCondWidth.W)
  val lt   = 8.U(branchCondWidth.W)
  val ltu  = 9.U(branchCondWidth.W)
  val ne   = 10.U(branchCondWidth.W)
}

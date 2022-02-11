package core.pipeline.stagereg

import chisel3._
import config.CpuConfig._

class IdExStage extends StageReg(new IdExBundle) {}

class IdExBundle extends Bundle {
  class ControlBundle extends Bundle {
    val regWriteEn    = Bool()
    val memToReg      = Bool()
    val storeMode     = UInt(storeModeWidth.W)
    val aluOp         = UInt(aluOpWidth.W)
    val aluYFromImm   = Bool()
    val aluXFromShamt = Bool()
  }

  val control      = new ControlBundle()
  val rs           = UInt(dataWidth.W)
  val rt           = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
  val immediate    = UInt(dataWidth.W)
  val shamt        = UInt(shamtWidth.W)
}

package core.execute.aluexec

import chisel3._
import config.CPUConfig._
import core.decode.StoreMode
import core.components.StageReg

class ExMemBundle extends Bundle {

  class ControlBundle extends Bundle {
    val regWriteEn = Bool()
    val loadMode   = Bool()
    val storeMode  = UInt(StoreMode.width.W)
  }

  val control      = new ControlBundle
  val aluResult    = UInt(dataWidth.W)
  val memWriteData = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
}

class ExMemStage extends StageReg(new ExMemBundle) {}

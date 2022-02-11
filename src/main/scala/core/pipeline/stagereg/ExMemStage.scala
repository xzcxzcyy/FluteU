package core.pipeline.stagereg

import chisel3._
import config.CpuConfig._

class ExMemBundle extends Bundle {

  class ControlBundle extends Bundle {
    val regWriteEn = Bool()
    val memToReg = Bool()
    val storeMode = UInt(storeModeWidth.W)
  }

  val control = new ControlBundle
  val aluResult = UInt(dataWidth.W)
  val memWriteData = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
}

class ExMemStage extends StageReg(new ExMemBundle) {}

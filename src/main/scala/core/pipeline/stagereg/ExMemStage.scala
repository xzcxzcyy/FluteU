package core.pipeline.stagereg

import chisel3._
import config.CpuConfig._
import core.components.StoreMode

class ExMemBundle extends Bundle {

  class ControlBundle extends Bundle {
    val regWriteEn = Bool()
    val memToReg = Bool()
    val storeMode = UInt(StoreMode.width.W)
  }

  val control = new ControlBundle
  val aluResult = UInt(dataWidth.W)
  val memWriteData = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
}

class ExMemStage extends StageReg(new ExMemBundle) {}

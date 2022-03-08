package flute.core.execute.aluexec

import chisel3._
import flute.config.CPUConfig._
import flute.core.decode.StoreMode
import flute.core.components.StageReg
import flute.core.decode.LoadMode

class ExMemBundle extends Bundle {

  class ControlBundle extends Bundle {
    val regWriteEn = Bool()
    val loadMode   = UInt(LoadMode.width.W)
    val storeMode  = UInt(StoreMode.width.W)
  }

  val control      = new ControlBundle
  val aluResult    = UInt(dataWidth.W)
  val memWriteData = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
  val branchValid  = Bool()
  val branchAddr   = UInt(addrWidth.W)
}

class ExMemStage extends StageReg(new ExMemBundle) {}

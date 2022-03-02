package flute.core.execute.aluexec

import chisel3._

import flute.core.components.StageReg
import flute.config.CPUConfig._

class MemWbBundle extends Bundle {
  val control = new Bundle {
    val regWriteEn = Bool()
    val memToReg   = Bool()
  }

  val aluResult    = UInt(dataWidth.W)
  val dataFromMem  = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
}

class MemWbStage extends StageReg(new MemWbBundle) {}

package pipeline

import chisel3._
import config.CpuConfig._


class IfIdBundle extends Bundle {
  val instruction = UInt(32.W)
  val pcplusfour  = UInt(32.W)
}

class IfIdStage extends StageReg(new IfIdBundle) {
}

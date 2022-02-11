package core.components

import chisel3._
import config.CpuConfig._

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

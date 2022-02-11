package core.pipeline

import chisel3._
import config.CpuConfig._
import core.pipeline.stagereg.MemWbBundle

class WriteBack extends Module {
  val io = IO(new Bundle {
    val fromMem       = Input(new MemWbBundle)
    val regWriteEn    = Output(Bool())
    val writeBackData = Output(UInt(dataWidth.W))
    val writeRegAddr  = Output(UInt(regAddrWidth.W))
  })
  io.regWriteEn    := io.fromMem.control.regWriteEn
  io.writeBackData := Mux(io.fromMem.control.memToReg, io.fromMem.dataFromMem, io.fromMem.aluResult)
  io.writeRegAddr  := io.fromMem.writeRegAddr
}

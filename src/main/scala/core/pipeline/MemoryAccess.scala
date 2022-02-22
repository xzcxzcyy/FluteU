package core.pipeline

import chisel3._
import mock.MockDataMem
import core.components.StoreMode
import core.pipeline.stagereg.{ExMemBundle, MemWbBundle}

class MemoryAccess(memFilePath: String) extends Module {
  val io = IO(new Bundle {
    val fromEx       = Input(new ExMemBundle())
    val toWb         = Output(new MemWbBundle())
    val dMemStallReq = Output(Bool())
  })

  io.toWb.control.regWriteEn := io.fromEx.control.regWriteEn
  io.toWb.control.memToReg   := io.fromEx.control.memToReg
  io.toWb.aluResult          := io.fromEx.aluResult
  io.toWb.writeRegAddr       := io.fromEx.writeRegAddr

  val dMem = Module(new MockDataMem(memFilePath))
  dMem.io.addr        := io.fromEx.aluResult
  dMem.io.dataIn      := io.fromEx.memWriteData
  dMem.io.enable      := 1.B
  dMem.io.write       := (io.fromEx.control.storeMode =/= StoreMode.disable)
  io.toWb.dataFromMem := dMem.io.dataOut

  io.dMemStallReq := io.fromEx.control.memToReg && (!dMem.io.valid)
}

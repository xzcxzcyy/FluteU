package pipeline

import chisel3._
import mock.MockDataMem
import config.CpuConfig.StoreMode

class MemoryAccess extends Module {
  val io = IO(new Bundle {
    val fromEx       = Input(new ExMemBundle())
    val toWb         = Output(new MemWbBundle())
    val dMemStallReq = Output(Bool())
  })

  io.toWb.control.regWriteEn := io.fromEx.control.regWriteEn
  io.toWb.control.memToReg   := io.fromEx.control.memToReg
  io.toWb.aluResult          := io.fromEx.aluResult
  io.toWb.writeRegAddr       := io.fromEx.writeRegAddr

  val dMem = Module(new MockDataMem("test_data/dmem.in"))
  dMem.io.addr        := io.fromEx.aluResult
  dMem.io.dataIn      := io.fromEx.memWriteData
  io.toWb.dataFromMem := dMem.io.dataOut
  dMem.io.enable      := 1.B
  dMem.io.write       := (io.fromEx.control.storeMode =/= StoreMode.disable)

  io.dMemStallReq := io.fromEx.control.memToReg && (!dMem.io.valid)
}

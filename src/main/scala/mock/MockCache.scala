package mock

import chisel3._

import config.CpuConfig._

class MockCache(content: Seq[Long]) extends Module {

  val io = IO(new Bundle {
    val readAddr  = Input(UInt(addrWidth.W))
    val writeAddr = Input(UInt(addrWidth.W))
    val storeMode = Input(UInt(storeModeWidth.W))
    val readData  = Output(UInt(dataWidth.W))
    val writeData = Input(UInt(dataWidth.W))
  })

  assert(content.length > 0)
  val hardContent = content.map(e => e.U(dataWidth.W))
  val ram         = RegInit(VecInit(hardContent))

  io.readData := ram(io.readAddr)
  when (io.storeMode =/= StoreMode.disable) {
    ram(io.writeAddr) := io.writeData
  }
}

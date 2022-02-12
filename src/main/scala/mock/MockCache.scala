package mock

import chisel3._
import config.CpuConfig._
import core.components.StoreMode

/**
  * Addresses MUST be aligned to 4 bytes
  * 
  * @param content
  */
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

  io.readData := ram(io.readAddr(31, 2))
  when (io.storeMode =/= StoreMode.disable) {
    ram(io.writeAddr(31, 2)) := io.writeData
  }
}

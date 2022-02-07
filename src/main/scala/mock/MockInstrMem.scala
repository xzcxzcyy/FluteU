package mock

import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import config.CpuConfig._

class MockInstrMem(memoryFile: String = "") extends Module {
  val width: Int = 32
  val io = IO(new Bundle {
    val addr = Input(UInt(addrWidth.W))
    val dataOut = Output(UInt(width.W))
    val ready = Output(Bool())
  })

  val mem = SyncReadMem(1024, UInt(width.W))
  val addr = RegInit(0.U(width.W))

  // Initialize memory
  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  val cut_addr = io.addr(31,2)

  io.ready := (cut_addr === addr)
  io.dataOut := mem(cut_addr)

  addr := cut_addr

  printf("Input is %d\t", io.addr)
  printf("dataOut is %x\t", io.dataOut)
  printf("ready is %d\n", io.ready)
}
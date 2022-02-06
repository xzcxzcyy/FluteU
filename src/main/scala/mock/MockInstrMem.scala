package mock

import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline

class MockInstrMem(memoryFile: String = "") extends Module {
  val width: Int = 32
  val io = IO(new Bundle {
    val addr = Input(UInt(10.W))
    val dataOut = Output(UInt(width.W))
  })

  val mem = SyncReadMem(1024, UInt(width.W))
  // Initialize memory
  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }
  io.dataOut := mem(io.addr >> 2)
  printf("Input is %d\n", io.addr)
  printf("Output is %x\n", io.dataOut)
}
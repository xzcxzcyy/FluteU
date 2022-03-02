package flute.mock

import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline

class MockDataMem(memoryFile: String = "") extends Module {
  val width: Int = 32
  val io = IO(new Bundle {
    val enable  = Input(Bool())
    val write   = Input(Bool())
    val addr    = Input(UInt(width.W))
    val dataIn  = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))

    val valid = Output(Bool())
  })

  val mem = SyncReadMem(1024, UInt(width.W))

  val cut_addr   = io.addr(31, 2)
  val addr       = RegInit(0.U(width.W))
  val write_done = RegInit(false.B)

  io.valid := !write_done && (cut_addr === addr)

  // Initialize memory
  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  io.dataOut := DontCare

  when(io.enable) {
    val rdwrPort = mem(cut_addr)
    when(io.write) {
      rdwrPort   := io.dataIn
      write_done := true.B
    }
      .otherwise {
        io.dataOut := rdwrPort
        addr       := cut_addr
        write_done := false.B
      }
  }
}

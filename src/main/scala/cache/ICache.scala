package cache

import config.CpuConfig._

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.DecoupledIO
import chisel3.util.experimental.loadMemoryFromFileInline

class ICacheIO extends Bundle {
  val addr = Flipped(new DecoupledIO(UInt(addrWidth.W)))
  val data = new DecoupledIO(Vec(fetchGroupSize, UInt(dataWidth.W)))
}

class ICache(memoryFile: String = "test_data/imem.in") extends Module {
  val io = IO(new ICacheIO)

  val memAddr = io.addr.bits(31, 2)

  val mem     = SyncReadMem(1024, UInt(dataWidth.W))
  val addrBuf = RegInit(0.U(addrWidth.W))
  val dataBuf = RegInit(VecInit(Seq.fill(fetchGroupSize)(0.U(instrWidth.W))))
  val state   = RegInit(0.U(2.W))

  val memPorts = for (i <- 0 to fetchGroupSize - 1) yield mem.read(memAddr + i.U)

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  when(state === 0.U) {
    when(io.addr.valid) {
      state   := 1.U
      addrBuf := io.addr.bits
    }.otherwise {
      state := 0.U
    }
  }.elsewhen(state === 1.U) {
    for (i <- 0 to fetchGroupSize - 1) yield dataBuf(i.U) := memPorts(i)
    when(io.addr.valid) {
      state   := 2.U
      addrBuf := io.addr.bits
    }.otherwise {
      state := 3.U
    }
  }.elsewhen(state === 2.U) {
    for (i <- 0 to fetchGroupSize - 1) yield dataBuf(i.U) := memPorts(i)
    when(io.addr.valid) {
      state   := 2.U
      addrBuf := io.addr.bits
    }.otherwise {
      state := 3.U
    }
  }.otherwise {
    when(io.addr.valid) {
      state   := 1.U
      addrBuf := io.addr.bits
    }.otherwise {
      state := 0.U
    }

  }

  io.data.valid := (state === 2.U || state === 3.U)
  io.addr.ready := 1.B
  io.data.bits  := dataBuf
}

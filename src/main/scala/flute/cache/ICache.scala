package flute.cache

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.DecoupledIO
import chisel3.util.Cat
import chisel3.util.experimental.loadMemoryFromFileInline

import flute.config.CPUConfig._
import flute.util.BitMode.fromIntToBitModeLong

class ICacheIO extends Bundle {
  val addr = Flipped(DecoupledIO(UInt(addrWidth.W)))
  val data = DecoupledIO(Vec(fetchGroupSize, UInt(dataWidth.W)))
}

/**
  * Currently, ICache does not check addr.valid, data.ready.
  * Icache also asserts addr.ready at all time.
  * The only useful port is data.valid
  *
  * @param memoryFile Path to memory file.
  */
class ICache(memoryFile: String = "test_data/imem.in") extends Module {
  val io = IO(new ICacheIO)

  val instInd = Cat(io.addr.bits(31, 2 + fetchGroupWidth), 0.U(fetchGroupWidth.W))

  val mem = Mem(1024, UInt(dataWidth.W))

  val memPorts = for (i <- 0 until fetchGroupSize) yield mem.read(instInd + i.U)

  val lastInstInd = RegInit(-1.BM.U(29, 0))
  lastInstInd := instInd

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  io.addr.ready := 1.B
  // io.data.valid := 1.B
  io.data.valid := (lastInstInd === instInd)
  for (i <- 0 to fetchGroupSize - 1) yield {
    io.data.bits(i.U) := memPorts(i)
  }
}

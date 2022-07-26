package flute.core.frontend

import chisel3._
import chisel3.util._
import flute.core.frontend._
import flute.cache.ICache
import flute.config.CPUConfig._
import flute.core.backend.commit.BranchCommit

class Frontend(nWays: Int = 2, memoryFile: String = "test_data/imem.in") extends Module {
  assert(nWays == 2)

  val io = IO(new Bundle {
    val out          = Vec(nWays, DecoupledIO(new IBEntry))
    val pc           = Output(UInt(addrWidth.W))
    val branchCommit = Input(new BranchCommit)
  })

  val fetch  = Module(new Fetch)
  val iCache = Module(new ICache(memoryFile))

  fetch.io.iCache <> iCache.io
  io.out <> fetch.io.withDecode.ibufferEntries
  fetch.io.cp0          := DontCare
  fetch.io.branchCommit := io.branchCommit

  io.pc := fetch.io.pc
}

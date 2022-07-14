package flute.core

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class BetaTop(memoryFile: String = "test_data/imem.in") extends Module {
  val io = IO(new Bundle {
    val pc        = Output(UInt(addrWidth.W))
    val prf       = Output(Vec(phyRegAmount, UInt(dataWidth.W)))
    val busyTable = Output(Vec(phyRegAmount, Bool()))
  })

  val frontend = Module(new Frontend(memoryFile = memoryFile))
  val backend  = Module(new Backend)

  for (i <- 0 until 2) {
    backend.io.ibuffer(i).bits  := frontend.io.out(i).bits
    frontend.io.out(i).ready    := backend.io.ibuffer(i).ready
    backend.io.ibuffer(i).valid := frontend.io.out(i).bits.inst.orR
  }
  io.pc        := frontend.io.pc
  io.prf       := backend.io.prf
  io.busyTable := backend.io.busyTable
}

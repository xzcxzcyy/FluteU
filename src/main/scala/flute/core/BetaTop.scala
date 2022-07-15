package flute.core

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.rename.ArfView

class BetaTop(memoryFile: String = "test_data/imem.in") extends Module {
  val io = IO(new Bundle {
    val pc  = Output(UInt(addrWidth.W))
    val arf = Output(Vec(archRegAmount, UInt(dataWidth.W)))
  })

  val frontend = Module(new Frontend(memoryFile = memoryFile))
  val backend  = Module(new Backend)

  backend.io.ibuffer <> frontend.io.out
  io.pc := frontend.io.pc

  val arfView = Module(new ArfView)
  arfView.io.rmtIn := backend.io.rmt
  arfView.io.prf   := backend.io.prf

  io.arf := arfView.io.arfOut

}

package flute.core.backend.rename

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._

class ArfView extends Module {
  val io = IO(new Bundle {
    val rmtIn  = Flipped(new RMTDebugOut)
    val arfOut = Output(Vec(archRegAmount, UInt(dataWidth.W)))
    val prf    = Input(Vec(phyRegAmount, UInt(dataWidth.W)))
  })

  for (i <- 0 until archRegAmount)  {
    io.arfOut(i) := io.prf(io.rmtIn.aRAT(i))
  }
}

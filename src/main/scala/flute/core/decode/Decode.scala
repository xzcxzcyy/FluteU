package flute.core.decode

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.components.{RegFile, RegFileWriteIO}
import flute.core.fetch.FetchIO
import flute.core.decode.issue.{FIFOQueue, Issue}

// the io that with next
class DecodeIO extends Bundle {
  val microOps = Vec(superscalar, DecoupledIO(new MicroOp))
}

class DecodeFeedbackIO extends Bundle {}

class Decode extends Module {
  val io = IO(new Bundle {
    val withExecute  = new DecodeIO()
    val withFetch    = Flipped(new FetchIO())
    val feedback     = new DecodeFeedbackIO()
    val regFileWrite = Vec(superscalar, new RegFileWriteIO())

    val debug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })

  val decoders = for (i <- 0 until 2) yield Module(new Decoder)

  val issueQueue = Module(new FIFOQueue(new MicroOp, 16, superscalar, decodeWay))

  val issuer = Module(new Issue)

  for (i <- 0 until decodeWay) {
    decoders(i).io.instr        := io.withFetch.ibufferEntries(i).bits
    issueQueue.io.write(i).bits := decoders(i).io.microOp

    // r/v
    io.withFetch.ibufferEntries(i).ready := issueQueue.io.write(i).ready
    issueQueue.io.write(i).valid         := io.withFetch.ibufferEntries(i).valid
  }

  issuer.io.fromId <> issueQueue.io.read

  io.withExecute.microOps <> issuer.io.toEx

  issuer.io.regFileWrite := io.regFileWrite
}

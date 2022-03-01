package core.decode

import chisel3._
import chisel3.util._
import config.CPUConfig._
import core.components.{RegFile, RegFileWriteIO}
import core.fetch.FetchIO
import core.execute.ExecutorIO

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
  })

  val regFile = Module(new RegFile(superscalar, superscalar))

  val decoders = for (i <- 0 until superscalar) yield Module(new Decoder)
  for (i <- 0 until superscalar) {
    // read
    regFile.io.read(i) <> decoders(i).io.withRegfile
    // write
    regFile.io.write(i) := io.regFileWrite
  }
}

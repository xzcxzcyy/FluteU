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

  val decoders = Vec(superscalar, new Decoder)
  for (i <- 0 until superscalar) {
    // read
    regFile.io.read(i).r1Addr := decoders(i).io.rsAddr
    regFile.io.read(i).r2Addr := decoders(i).io.rtAddr
    decoders(i).io.rsData := regFile.io.read(i).r1Data
    decoders(i).io.rtData := regFile.io.read(i).r2Data

    // write
    regFile.io.write(i).writeEnable := io.regFileWrite(i).writeEnable
    regFile.io.write(i).writeAddr := io.regFileWrite(i).writeAddr
    regFile.io.write(i).writeData := io.regFileWrite(i).writeData
  }
}

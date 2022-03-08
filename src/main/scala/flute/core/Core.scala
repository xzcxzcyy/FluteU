package flute.core

import chisel3._

import flute.config.CPUConfig._
import flute.cache._
import flute.core.fetch._
import flute.core.decode._
import flute.core.execute._

class Core extends Module {
  val io = IO(new Bundle {
    val iCache = Flipped(new ICacheIO)
    val dCache = Flipped(Vec(superscalar,new DCacheIO))
    // val rFdebug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })

  val fetch   = Module(new Fetch())
  val decode  = Module(new Decode())
  val execute = Module(new Execute())

  // io.rFdebug := decode.io.debug

  fetch.io.withDecode <> decode.io.withFetch
  decode.io.withExecute <> execute.io.withDecode
  decode.io.regFileWrite := execute.io.withRegFile
  io.dCache <> execute.io.dCache
  fetch.io.iCache <> io.iCache
  fetch.io.feedbackFromDecode <> decode.io.feedback
  fetch.io.feedbackFromExec <> execute.io.feedback
}

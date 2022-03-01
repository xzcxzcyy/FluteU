package core

import chisel3._

import config.CPUConfig._
import cache._
import core.fetch._
import core.decode._
import core.execute._

class Core extends Module {
  val io = IO(new Bundle {
    val iCache = Flipped(new ICacheIO)
    val dCache = new DCacheIO
    val rFdebug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })

  val fetch   = Module(new Fetch())
  val decode  = Module(new Decode())
  val execute = Module(new Execute())

  io.rFdebug := decode.io.debug

  fetch.io.withDecode <> decode.io.withFetch
  decode.io.withExecute <> execute.io.withDecode
  decode.io.regFileWrite := execute.io.withRegFile
  io.dCache := DontCare // TODO: connect dcache here
  fetch.io.iCache <> io.iCache
  fetch.io.feedbackFromDecode <> decode.io.feedback
  fetch.io.feedbackFromExec <> execute.io.feedback
}

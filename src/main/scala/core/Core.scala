package core

import chisel3._

import config.CPUConfig._
import cache._
import core.fetch._
import core.decode._
import core.execute._

class Core extends Module {
  val io = IO(new Bundle {
    val iCache = new ICacheIO
    val dCache = new DCacheIO
  })

  val fetch   = Module(new Fetch())
  val decode  = Module(new Decode())
  val execute = Module(new Execute())

  fetch.io.withDecode <> decode.io.withFetch
  decode.io.withExecute <> execute.io.withDecode
  // connect cache here
  fetch.io.feedbackFromDecode <> decode.io.feedback
  fetch.io.feedbackFromExec <> execute.io.feedback
}

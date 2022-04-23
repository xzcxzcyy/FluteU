package flute.core

import chisel3._

import flute.config.CPUConfig._
import flute.cache._
import flute.core.fetch._
import flute.core.decode._
import flute.core.execute._
import flute.cp0.CP0WithCore

class Core extends Module {
  val io = IO(new Bundle {
    val cp0    = Flipped(new CP0WithCore)
    val iCache = Flipped(new ICacheIO)
    val dCache = Flipped(Vec(superscalar, new DCacheIO))
    val debug  = Output(Vec(regAmount, UInt(dataWidth.W)))
    val pc     = Output(UInt(addrWidth.W))
  })

  val fetch   = Module(new Fetch())
  val decode  = Module(new Decode())
  val execute = Module(new Execute())

  io.debug := decode.io.debug

  fetch.io.withDecode <> decode.io.withFetch
  decode.io.withExecute <> execute.io.withDecode
  decode.io.regFileWrite := execute.io.withRegFile
  io.dCache <> execute.io.dCache
  fetch.io.iCache <> io.iCache
  fetch.io.feedbackFromDecode <> decode.io.feedback
  fetch.io.feedbackFromExec <> execute.io.feedback

  io.pc := fetch.io.pc

  io.cp0.write.enable := 0.B
  io.cp0.write.addr   := 0.U
  io.cp0.write.sel    := 0.U
  io.cp0.write.data   := 0.U
  io.cp0.read.addr    := 0.U
  io.cp0.read.sel     := 0.U
}

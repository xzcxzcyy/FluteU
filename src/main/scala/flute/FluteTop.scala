package flute

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.backend.rename.ArfView
import flute.cp0.CP0
import chisel3.stage.ChiselStage
import flute.cache.top.DCacheWithCore
import flute.cache.top.DataCache
import flute.config.CacheConfig
import flute.core.backend.Backend
import flute.core.frontend.Frontend
import flute.cache.ICache
import flute.cache.top.ThroughDCache
import flute.cache.top.ThroughICache
import flute.axi.AXIIO
import flute.cache.axi.AXIReadArbiter
import flute.core.backend.TraceBundle
import flute.cp0.CP0DebugIO

class FluteTop extends Module {
  val io = IO(new Bundle {
    val hwIntr = Input(UInt(6.W))
    val pc     = Output(UInt(addrWidth.W))
    val arf    = Output(Vec(archRegAmount, UInt(dataWidth.W)))
    val axi    = AXIIO.master()
    // Debug
    val count     = Output(UInt(dataWidth.W))
    val arfWTrace = Output(new TraceBundle)
    val cp0Debug = Output(new CP0DebugIO)
  })

  val frontend = Module(new Frontend)
  val backend  = Module(new Backend)
  val cp0      = Module(new CP0)
  val dCache   = Module(new ThroughDCache)
  val iCache   = Module(new ThroughICache)

  val axiReadArbiter = Module(new AXIReadArbiter(masterCount = 2))
  axiReadArbiter.io.bus.aw := DontCare
  axiReadArbiter.io.bus.w  := DontCare
  axiReadArbiter.io.bus.b  := DontCare

  axiReadArbiter.io.masters(0) <> dCache.io.axi
  axiReadArbiter.io.masters(1) <> iCache.io.axi
  io.axi.ar <> axiReadArbiter.io.bus.ar
  io.axi.r <> axiReadArbiter.io.bus.r
  io.axi.aw <> dCache.io.axi.aw
  io.axi.w <> dCache.io.axi.w
  io.axi.b <> dCache.io.axi.b

  backend.io.ibuffer <> frontend.io.out
  frontend.io.branchCommit := backend.io.branchCommit
  frontend.io.cp0.epc      := cp0.io.core.epc
  frontend.io.cp0.eretReq  := backend.io.cp0.eret
  frontend.io.cp0.intrReq  := cp0.io.core.intrReq
  frontend.io.icache <> iCache.io.core
  io.pc         := frontend.io.pc
  cp0.io.hwIntr := io.hwIntr

  cp0.io.core.read <> backend.io.cp0Read
  cp0.io.core.write := backend.io.cp0Write

  backend.io.cp0IntrReq := cp0.io.core.intrReq
  backend.io.cp0 <> cp0.io.core.commit
  backend.io.dcache <> dCache.io.core

  val arfView = Module(new ArfView)
  arfView.io.rmtIn := backend.io.rmt
  arfView.io.prf   := backend.io.prf

  // DEBUG //
  io.count     := cp0.io.debug.count
  io.arfWTrace := backend.io.arfWTrace
  io.cp0Debug  := cp0.io.debug
  dontTouch(io.cp0Debug)
  // ===== //

  io.arf := arfView.io.arfOut

}

object FluteGen extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new FluteTop, Array("--target-dir", "target/verilog/flute", "--target:fpga"))
}

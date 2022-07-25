package flute.core

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.backend.rename.ArfView
import flute.cp0.CP0
import chisel3.stage.ChiselStage
import flute.cache.top.DCachePorts
import flute.cache.top.DataCache
import flute.config.CacheConfig
import flute.core.backend.Backend

class BetaTop(iFile: String, dFile: String) extends Module {
  val io = IO(new Bundle {
    val hwIntr = Input(UInt(6.W))
    val pc     = Output(UInt(addrWidth.W))
    val arf    = Output(Vec(archRegAmount, UInt(dataWidth.W)))
    val count  = Output(UInt(dataWidth.W))
  })

  val frontend = Module(new Frontend(memoryFile = iFile))
  val backend  = Module(new Backend)
  val cp0      = Module(new CP0)
  val dcache   = Module(new DataCache(new CacheConfig, dFile))

  backend.io.ibuffer <> frontend.io.out
  frontend.io.branchCommit := backend.io.branchCommit
  io.pc                    := frontend.io.pc
  cp0.io.hwIntr            := io.hwIntr
  // TEMP //
  cp0.io.core.read         := DontCare
  cp0.io.core.write        := DontCare
  cp0.io.core.write.enable := 0.B
  // ==== //
  backend.io.cp0IntrReq := cp0.io.core.intrReq
  backend.io.cp0 <> cp0.io.core.commit
  backend.io.dcache <> dcache.io

  val arfView = Module(new ArfView)
  arfView.io.rmtIn := backend.io.rmt
  arfView.io.prf   := backend.io.prf

  // DEBUG //
  io.count := cp0.io.debug.count
  // ===== //

  io.arf := arfView.io.arfOut

}

object BetaTopGen extends App {
  println("===== BataTop Gen Start =====")
  (new ChiselStage).emitVerilog(
    new BetaTop("test_data/xor.in", "test_data/zero.in"),
    Array("--target-dir", "target/verilog", "--target:fpga")
  )
  println("===== BataTop Gen Complete =====")
}

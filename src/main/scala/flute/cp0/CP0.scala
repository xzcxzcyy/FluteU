package flute.cp0

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._

class CP0Read extends Bundle {
  val addr = Input(UInt(5.W))
  val sel  = Input(UInt(3.W))
  val data = Output(UInt(dataWidth.W))
}

class CP0Write extends Bundle {
  val addr   = Input(UInt(5.W))
  val sel    = Input(UInt(3.W))
  val data   = Input(UInt(dataWidth.W))
  val enable = Input(Bool())
}

class CP0WithCommit extends Bundle {
  val pc     = Input(UInt(addrWidth.W))
  val inSlot = Input(Bool())
}

class CP0 extends Module {
  val io = IO(new Bundle {
    val read    = new CP0Read
    val write   = new CP0Write
    val commit  = new CP0WithCommit
    val hwIntr  = Input(UInt(6.W))
    val intrReq = Output(Bool())
  })

  val badvaddr = new CP0BadVAddr
  val count    = new CP0Count
  val status   = new CP0Status
  val cause    = new CP0Cause
  val epc      = new CP0EPC
}

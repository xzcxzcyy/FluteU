package flute.core.components

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._

class SbufferEntry extends Bundle {
  val addr  = UInt(addrWidth.W)
  val data  = UInt(dataWidth.W)
  val valid = Bool()
}

class SbufferWrite(entryAmount: Int) extends Bundle {
  val robAddr = Input(UInt(log2Up(entryAmount).W))
  val memAddr = Input(UInt(addrWidth.W))
  val memData = Input(UInt(dataWidth.W))
  val valid   = Input(Bool())
}

class SbufferRead(entryAmount: Int) extends Bundle {
  val memAddr = Input(UInt(log2Up(entryAmount).W))
  val valid   = Output(Bool())
  val data    = Output(UInt(dataWidth.W))
}

class Sbuffer(entryAmount: Int) extends Module {

  val io = IO(new Bundle {
    val write  = new SbufferWrite(entryAmount)
    val read   = new SbufferRead(entryAmount)
    val retire = Flipped(ValidIO(UInt(log2Up(entryAmount).W)))
  })

  val entries = Mem(entryAmount, new SbufferEntry)
  when(io.write.valid) {
    entries(io.write.robAddr).valid := 1.B
    entries(io.write.robAddr).addr  := io.write.memAddr
    entries(io.write.robAddr).data  := io.write.memData
  }
  for (i <- 0 until entryAmount) yield {
    when(io.write.valid && entries(i).addr === io.write.memAddr && i.U =/= io.write.robAddr) {
      entries(i).valid := 0.B
    }
  }

  val sbReadData  = WireInit(0.U(dataWidth.W))
  val sbReadValid = WireInit(false.B)
  for (i <- 0 until entryAmount) {
    when(entries(i).valid && entries(i).addr === io.read.memAddr) {
      sbReadData  := entries(i).data
      sbReadValid := 1.B
    }
  }
  io.read.valid := sbReadValid
  io.read.data  := sbReadData

  when(io.retire.valid) {
    entries(io.retire.bits).valid := 0.B
  }
}

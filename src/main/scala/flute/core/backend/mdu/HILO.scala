package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.util.ValidBundle

class HILORead extends Bundle {
  val hi = UInt(dataWidth.W)
  val lo = UInt(dataWidth.W)
}

class HILOWrite extends Bundle {
  val hi = ValidBundle(UInt(dataWidth.W))
  val lo = ValidBundle(UInt(dataWidth.W))
}

// 无写后读冲突
class HILO extends Module {
  val io = IO(new Bundle {
    val read  = Output(new HILORead)
    val write = Input(new HILOWrite)
  })

  val hi = RegInit(0.U(dataWidth.W))
  val lo = RegInit(0.U(dataWidth.W))

  io.read.hi := hi
  io.read.lo := lo

  when(io.write.hi.valid) {
    hi := io.write.hi.bits
  }

  when(io.write.lo.valid) {
    lo := io.write.lo.bits
  }

}

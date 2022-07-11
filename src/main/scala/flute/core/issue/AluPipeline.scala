package flute.core.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp
import flute.config.CPUConfig._
import flute.core.rob._
import flute.core.components._

class AluWB extends Bundle {
  val rob = new ROBCompleteBundle(robEntryNumWidth)
  val prf = new RegFileWriteIO

  val busyTable = Valid(UInt(phyRegAddrWidth.W))
}

class BypassBundle extends Bundle {
  val in  = Input(Vec(2, UInt(dataWidth.W)))
  val out = Output(UInt(dataWidth.W))
}

class AluPipeline extends Module {
  val io = IO(new Bundle {
    // 无阻塞
    val uop = Input(Valid(new AluEntry))
    val prf = Flipped(new RegFileReadIO)
    val wb  = Output(new AluWB)

    val bypass = new BypassBundle
  })
}

package flute.core.execute

import chisel3._
import chisel3.util.MuxLookup

import flute.config._
import flute.cache.DCacheIO

class ExecuteIO(implicit conf:CPUConfig) extends Bundle {
}

class ExecuteFeedbackIO(implicit conf:CPUConfig) extends Bundle {
}


class Execute(implicit conf:CPUConfig) extends Module {
  val io = IO(new Bundle {
    val execute  = new ExecuteIO()
    val feedback = new ExecuteFeedbackIO()
    val dCache   = new DCacheIO()
  })
}
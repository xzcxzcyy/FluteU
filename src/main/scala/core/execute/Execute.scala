package core.execute

import chisel3._
import chisel3.util.MuxLookup

import config.CPUConfig._
import cache.DCacheIO

class ExecuteIO extends Bundle {}

class ExecuteFeedbackIO extends Bundle {}

class Execute extends Module {
  val io = IO(new Bundle {
    val execute  = new ExecuteIO()
    val feedback = new ExecuteFeedbackIO()
    val dCache   = new DCacheIO()
  })
}

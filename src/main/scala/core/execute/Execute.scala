package core.execute

import chisel3._
import chisel3.util.MuxLookup

import config.CPUConfig._
import cache.DCacheIO
import core.decode.DecodeIO

class ExecutorIO extends Bundle {}

class ExecuteFeedbackIO extends Bundle {}

class Execute extends Module {
  val io = IO(new Bundle {
    val withDecode = Flipped(new DecodeIO())
    val feedback   = new ExecuteFeedbackIO()
    val dCache     = Vec(superscalar, Flipped(new DCacheIO()))
  })


  val idExStage = Module(new IdExStage)
  // 依据 io.withDecode.microOps(0).valid 判断输入数据是否有效
  // 提供 io.withDecode.microOps(0).ready
  // 若流水畅通 ready 为 1; 流水拥塞 ready 为 0
  // 理想流水暂不考虑
  idExStage.io.in := io.withDecode.microOps(0).bits
  // xx := idExStage.io.data.bits
 
}

package flute.core.execute

import chisel3._
import chisel3.util.{MuxLookup,Valid}

import flute.config.CPUConfig._
import flute.cache.DCacheIO
import flute.core.decode.DecodeIO
import flute.core.components.{RegFileWriteIO, ALU}
import flute.core.execute.aluexec.ALUExecutor

class ExecuteFeedbackIO extends Bundle {
  val branchAddr = Valid(UInt(instrWidth.W))
}

class Execute extends Module {
  val io = IO(new Bundle {
    val withDecode  = Flipped(new DecodeIO())
    val withRegFile = Vec(superscalar, Flipped(new RegFileWriteIO()))
    val feedback    = new ExecuteFeedbackIO()
    val dCache     = Vec(superscalar, Flipped(new DCacheIO()))
  })

  // 依据 io.withDecode.microOps(0).valid 判断输入数据是否有效
  // 提供 io.withDecode.microOps(0).ready
  // 若流水畅通 ready 为 1; 流水拥塞 ready 为 0
  // 理想流水暂不考虑
  // xx := idExStage.io.data.bits
  val aluExecutors = for (i <- 0 until superscalar) yield Module(new ALUExecutor)
  io.feedback.branchAddr.valid := 0.B
  io.feedback.branchAddr.bits  := DontCare
  for (i <- 0 until superscalar) {
    when(aluExecutors(i).io.feedback.branchAddr.valid){
      io.feedback.branchAddr <> aluExecutors(i).io.feedback.branchAddr
    }
    aluExecutors(i).io.source <> io.withDecode.microOps(i)
    io.dCache(i) <> aluExecutors(i).io.dCache
    io.withRegFile(i) <> aluExecutors(i).io.regFile
  }
}

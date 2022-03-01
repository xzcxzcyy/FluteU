package core.execute

import chisel3._
import chisel3.util.MuxLookup

import config.CPUConfig._
import cache.DCacheIO
import core.decode.DecodeIO
import core.components.{RegFileWriteIO, ALU}
import core.execute.aluexec.ALUExecutor

class ExecuteFeedbackIO extends Bundle {}

class Execute extends Module {
  val io = IO(new Bundle {
    val withDecode  = Flipped(new DecodeIO())
    val withRegFile = Vec(superscalar, Flipped(new RegFileWriteIO()))
    val feedback    = new ExecuteFeedbackIO()
    // val dCache     = Vec(superscalar, Flipped(new DCacheIO()))
  })

  // val idExStages = for(i<-0 until superscalar) yield{ Module(new IdExStage) }
  // val alus = for(i<- 0 until superscalar) yield { Module(new ALU) }
  // 依据 io.withDecode.microOps(0).valid 判断输入数据是否有效
  // 提供 io.withDecode.microOps(0).ready
  // 若流水畅通 ready 为 1; 流水拥塞 ready 为 0
  // 理想流水暂不考虑
  // xx := idExStage.io.data.bits
  val aluExecutors = for (i <- 0 until superscalar) yield Module(new ALUExecutor)
  for (i <- 0 until superscalar) {
    aluExecutors(i).io.source <> io.withDecode.microOps(i)
    io.withRegFile(i) <> aluExecutors(i).io.regFile
  }
}

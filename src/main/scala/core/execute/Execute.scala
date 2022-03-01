package core.execute

import chisel3._
import chisel3.util.MuxLookup

import config.CPUConfig._
import cache.DCacheIO
import core.decode.DecodeIO
import core.components.{RegFileIO,ALU}

class ExecutorIO extends Bundle {
  val withRF = Vec(superscalar,Flipped(new RegFileIO()))
}

class ExecuteFeedbackIO extends Bundle {}

class Execute extends Module {
  val io = IO(new Bundle {
    val withDecode = Flipped(new DecodeIO())
    val withWb     = new ExecutorIO()
    val feedback   = new ExecuteFeedbackIO()
    // val dCache     = Vec(superscalar, Flipped(new DCacheIO()))
  })


  // val idExStage = Module(new IdExStage)
  // val idExStages = Vec(superscalar,Module(new IdExStage))
  val idExStages = for(i<-0 until superscalar) yield{Module(new IdExStage)}
  // val alus = Vec(superscalar,Module(new ALU))
  val alus = for(i<- 0 until superscalar) yield {Module(new ALU)}
  // 依据 io.withDecode.microOps(0).valid 判断输入数据是否有效
  // 提供 io.withDecode.microOps(0).ready
  // 若流水畅通 ready 为 1; 流水拥塞 ready 为 0
  // 理想流水暂不考虑
  // xx := idExStage.io.data.bits
  for(i <- 0 until superscalar){
    idExStages(i).io.in    := io.withDecode.microOps(i).bits
    idExStages(i).io.valid := io.withDecode.microOps(i).valid
    idExStages(i).io.flush := 0.B
    io.withDecode.microOps(i).ready := 1.B// optimal pipeline
    
    val data = idExStages(i).io.data
    
    alus(i).io.aluOp := data.controlSig.aluOp
    alus(i).io.x     := Mux(data.controlSig.aluXFromShamt,data.shamt,data.rs)
    alus(i).io.y     := Mux(data.controlSig.aluYFromImm,data.immediate,data.rt)

    io.withWb.withRF(i).writeAddr   := data.writeRegAddr
    io.withWb.withRF(i).writeEnable := data.controlSig.regWriteEn
    io.withWb.withRF(i).writeData   := alus(i).io.result
    io.withWb.withRF(i).r1Addr      := DontCare
    io.withWb.withRF(i).r2Addr      := DontCare
  }
}

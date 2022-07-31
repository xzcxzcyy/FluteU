package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.core.components.RegFileReadIO
import flute.core.components.HiLoIO
import flute.core.components.StageReg


class MduRead extends Module {
  val io = IO(new Bundle { 
    val in        = Input(Valid(new MicroOp(rename = true)))
    val out       = Output(Valid(new MicroOp(rename = true)))

    val mdPlReady = Input(Bool())
    val prf       = Flipped(new RegFileReadIO)
    val hilo      = Flipped(new HiLoIO)
  })

  io.prf.r1Addr := io.in.bits.rsAddr 
  io.prf.r2Addr := io.in.bits.rtAddr 

  val read2Ex = WireInit(io.in.bits)
  read2Ex.op1.op := io.prf.r1Data 
  read2Ex.op2.op := io.prf.r2Data 

  val stage_read = Module(new StageReg(Valid(new MicroOp(rename = true))))
  stage_read.io.in.bits := read2Ex 
  stage_read.io.in.valid := io.in.valid 
  io.out.bits := stage_read.io.data.bits 
  io.out.valid := stage_read.io.data.valid 
  stage_read.io.flush := false.B 
  stage_read.io.valid := io.mdPlReady
}

package core.execute.aluexec

import chisel3._
import chisel3.util._

import core.decode.MicroOp
import core.components.RegFileWriteIO
import core.components.ALU

class ALUExecutor extends Module {
  val io = IO(new Bundle {
    val source  = Flipped(DecoupledIO(new MicroOp))
    val regFile = Flipped(new RegFileWriteIO)
  })

  val idEx  = Module(new IdExStage)
  val exMem = Module(new ExMemStage)
  val memWb = Module(new MemWbStage)
  val alu   = Module(new ALU)

  // When not valid, insert a bubble.
  idEx.io.flush  := !io.source.valid
  idEx.io.valid  := 1.B
  exMem.io.valid := 1.B
  memWb.io.valid := 1.B
  exMem.io.flush := 0.B
  memWb.io.flush := 0.B

  io.source.ready := 1.B

  idEx.io.in := io.source.bits

  alu.io.aluOp := idEx.io.data.controlSig.aluOp
  alu.io.x     := Mux(idEx.io.data.controlSig.aluXFromShamt, idEx.io.data.shamt, idEx.io.data.rs)
  alu.io.y     := Mux(idEx.io.data.controlSig.aluYFromImm, idEx.io.data.immediate, idEx.io.data.rt)

  exMem.io.in.aluResult    := alu.io.result
  exMem.io.in.writeRegAddr := idEx.io.data.writeRegAddr
  exMem.io.in.memWriteData := idEx.io.data.rt
  exMem.io.in.control.loadMode := idEx.io.data.controlSig.loadMode
  exMem.io.in.control.regWriteEn := idEx.io.data.controlSig.regWriteEn
  exMem.io.in.control.storeMode  := idEx.io.data.controlSig.storeMode

  memWb.io.in.aluResult          := exMem.io.data.aluResult
  memWb.io.in.dataFromMem        := DontCare // TODO: add a dCache
  memWb.io.in.writeRegAddr       := exMem.io.data.writeRegAddr
  memWb.io.in.control.memToReg   := exMem.io.data.control.loadMode =/= 0.B // TODO: re-write load mode
  memWb.io.in.control.regWriteEn := exMem.io.data.control.regWriteEn

  io.regFile.writeEnable := memWb.io.data.control.regWriteEn
  io.regFile.writeData := Mux(
    memWb.io.data.control.memToReg,
    memWb.io.data.dataFromMem,
    memWb.io.data.aluResult
  )
  io.regFile.writeAddr := memWb.io.data.writeRegAddr
}

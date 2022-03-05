package flute.core.execute.aluexec

import chisel3._
import chisel3.util._

import flute.core.decode.MicroOp
import flute.core.components.RegFileWriteIO
import flute.core.components.ALU
import flute.cache.DCacheIO
import flute.core.decode.{StoreMode, BJCond}
import flute.core.execute.ExecuteFeedbackIO

class ALUExecutor extends Module {
  val io = IO(new Bundle {
    val source   = Flipped(DecoupledIO(new MicroOp))
    val dCache   = Flipped(new DCacheIO)
    val regFile  = Flipped(new RegFileWriteIO)
    val feedback = new ExecuteFeedbackIO
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

  val branchTaken = MuxLookup(
    key = idEx.io.data.controlSig.bjCond,
    default = 0.B,
    mapping = Seq(
      BJCond.eq   -> alu.io.flag.equal,
      BJCond.ge   -> !alu.io.flag.lessS,
      BJCond.gez  -> !alu.io.flag.lessS, // rs-0
      BJCond.geu  -> !alu.io.flag.lessU,
      BJCond.gt   -> !(alu.io.flag.lessS | alu.io.flag.equal),
      BJCond.gtu  -> !(alu.io.flag.lessU | alu.io.flag.equal),
      BJCond.gtz  -> !(alu.io.flag.lessS | alu.io.flag.equal),
      BJCond.le   -> (alu.io.flag.lessS | alu.io.flag.equal),
      BJCond.leu  -> (alu.io.flag.lessU | alu.io.flag.equal),
      BJCond.lez  -> (alu.io.flag.lessS | alu.io.flag.equal),
      BJCond.lt   -> alu.io.flag.lessS,
      BJCond.ltu  -> alu.io.flag.lessU,
      BJCond.ltz  -> alu.io.flag.lessS,
      BJCond.ne   -> !alu.io.flag.equal,
      BJCond.none -> 0.B
    )
  )
  io.feedback.branchAddr.valid := idEx.io.data.controlSig.bjCond =/= BJCond.none
  io.feedback.branchAddr.bits := Mux(
    branchTaken,
    idEx.io.data.pc + 4.U + Cat(idEx.io.data.immediate, 0.U(2.W)),
    idEx.io.data.pc + 8.U
  )

  exMem.io.in.aluResult          := alu.io.result
  exMem.io.in.writeRegAddr       := idEx.io.data.writeRegAddr
  exMem.io.in.memWriteData       := idEx.io.data.rt
  exMem.io.in.control.loadMode   := idEx.io.data.controlSig.loadMode
  exMem.io.in.control.regWriteEn := idEx.io.data.controlSig.regWriteEn
  exMem.io.in.control.storeMode  := idEx.io.data.controlSig.storeMode

  io.dCache.addr      := exMem.io.data.aluResult
  io.dCache.writeData := exMem.io.data.memWriteData
  io.dCache.storeMode := exMem.io.data.control.storeMode

  memWb.io.in.aluResult        := exMem.io.data.aluResult
  memWb.io.in.dataFromMem      := io.dCache.readData
  memWb.io.in.writeRegAddr     := exMem.io.data.writeRegAddr
  memWb.io.in.control.memToReg := exMem.io.data.control.loadMode =/= 0.B // TODO: re-write load mode
  memWb.io.in.control.regWriteEn := exMem.io.data.control.regWriteEn

  io.regFile.writeEnable := memWb.io.data.control.regWriteEn
  io.regFile.writeData := Mux(
    memWb.io.data.control.memToReg,
    memWb.io.data.dataFromMem,
    memWb.io.data.aluResult
  )
  io.regFile.writeAddr := memWb.io.data.writeRegAddr
}

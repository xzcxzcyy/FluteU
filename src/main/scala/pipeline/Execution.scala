package pipeline

import chisel3._
import chisel3.util.MuxLookup
import components.ALU
import config.CpuConfig._

class Execution extends Module {
  val io = IO(new Bundle {
    val fromId = Input(new IdExBundle)
    val toMem  = Output(new ExMemBundle)
  })

  io.toMem.control.memToReg   := io.fromId.control.memToReg
  io.toMem.control.memWrite   := io.fromId.control.memWrite
  io.toMem.control.regWriteEn := io.fromId.control.regWriteEn

  val alu = Module(new ALU())
  alu.io.aluOp          := io.fromId.control.aluOp
  alu.io.x              := Mux(io.fromId.control.aluXFromShamt, io.fromId.shamt, io.fromId.rs)
  alu.io.y              := Mux(io.fromId.control.aluYFromImm, io.fromId.immediate, io.fromId.rt)
  io.toMem.aluResult    := alu.io.result
  io.toMem.memWriteData := io.fromId.rt
  io.toMem.writeRegAddr := io.fromId.writeRegAddr

  /* val branchTaken = MuxLookup(
    key = io.fromId.control.branchCond,
    default = 0.B,
    mapping = Seq(
      BranchCond.eq   -> alu.io.flag.equal,
      BranchCond.ge   -> !alu.io.flag.lessS,
      BranchCond.geu  -> !alu.io.flag.lessU,
      BranchCond.gt   -> !(alu.io.flag.lessS | alu.io.flag.equal),
      BranchCond.gtu  -> !(alu.io.flag.lessU | alu.io.flag.equal),
      BranchCond.le   -> (alu.io.flag.lessS | alu.io.flag.equal),
      BranchCond.leu  -> (alu.io.flag.lessU | alu.io.flag.equal),
      BranchCond.lt   -> alu.io.flag.lessS,
      BranchCond.ltu  -> alu.io.flag.lessU,
      BranchCond.ne   -> !alu.io.flag.equal,
      BranchCond.none -> 0.B,
    )
  ) */
}

package pipeline

import chisel3._
import chisel3.util.MuxLookup
import components.ALU

class Execution extends Module {
  val io = IO(new Bundle {
    val fromId      = Input(new IdExBundle)
    val toMem       = Output(new ExMemBundle)
    val wrongBranch = Output(Bool())
  })

  io.toMem.control.memToReg   := io.fromId.control.memToReg
  io.toMem.control.memWrite   := io.fromId.control.memWrite
  io.toMem.control.regWriteEn := io.fromId.control.regWriteEn

  val alu = new ALU
  alu.io.aluOp := io.fromId.control.aluOp
  alu.io.x     := Mux(io.fromId.control.aluXFromShamt, io.fromId.shamt, io.fromId.rs)
  alu.io.y     := Mux(io.fromId.control.aluYFromImm, io.fromId.immediate, io.fromId.rt)

  val branchTaken = MuxLookup(
    key = io.fromId.control.branchCond,
    default = 0.B,
    mapping = Seq()
  )
}

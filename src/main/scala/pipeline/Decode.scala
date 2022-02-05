package pipeline

import chisel3._
import chisel3.util.MuxLookup
import components.Comparator
import components.Controller
import components.RegFile
import config.CpuConfig._

class Decode extends Module{
  val io = IO(new Bundle {
    val fromIf = Input(new IfIdBundle)
    val toEx = Output(new IdExBundle)
  })


  val comparator = Module(new Comparator())
  val controller = Module(new Controller())
  val regFile    = Module(new RegFile())

  controller.io.instruction := io.fromIf.instruction
  io.toEx.control.regWriteEn := controller.io.regWriteEn
  io.toEx.control.memToReg := controller.io.memToReg
  io.toEx.control.storeMode := controller.io.storeMode
  io.toEx.control.aluOp := controller.io.aluOp
  io.toEx.control.aluXFromShamt := controller.io.aluXFromShamt
  io.toEx.control.aluYFromImm := controller.io.aluYFromImm

  regFile.io.r1Addr := io.fromIf.instruction(25, 21)  // rs
  regFile.io.r2Addr := io.fromIf.instruction(20, 16)  // rt
  io.toEx.rs := regFile.io.r1Data
  io.toEx.rt := regFile.io.r2Data

  io.toEx.writeRegAddr := MuxLookup(
    key = controller.io.regDst,
    default = 0.B,
    mapping = Seq(
      RegDst.rt -> io.fromIf.instruction(20, 16),
      RegDst.rd -> io.fromIf.instruction(15, 11),
      RegDst.GPR31 -> 31.U(regAddrWidth.W)
    )
  )

  val branchTaken = MuxLookup(
    key = controller.io.branchCond,
    default = 0.B,
    mapping = Seq(
      BranchCond.eq   -> comparator.io.flag.equal,
      BranchCond.ge   -> !comparator.io.flag.lessS,
      BranchCond.gez  -> !comparator.io.branchSig.rsSignBit,
      BranchCond.geu  -> !comparator.io.flag.lessU,
      BranchCond.gt   -> !(comparator.io.flag.lessS | comparator.io.flag.equal),
      BranchCond.gtz  -> comparator.io.branchSig.rsGtz,
      BranchCond.gtu  -> !(comparator.io.flag.lessU | comparator.io.flag.equal),
      BranchCond.le   -> (comparator.io.flag.lessS | comparator.io.flag.equal),
      BranchCond.lez  -> !comparator.io.branchSig.rsGtz,
      BranchCond.leu  -> (comparator.io.flag.lessU | comparator.io.flag.equal),
      BranchCond.lt   -> comparator.io.flag.lessS,
      BranchCond.ltz  -> comparator.io.branchSig.rsSignBit,
      BranchCond.ltu  -> comparator.io.flag.lessU,
      BranchCond.ne   -> !comparator.io.flag.equal,
      BranchCond.none -> 0.B,
      BranchCond.all  -> 1.B
    )
  )
}



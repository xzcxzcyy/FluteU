package pipeline


import Chisel.Cat
import chisel3._
import chisel3.util.MuxLookup
import components.Comparator
import components.Controller
import components.RegFile
import config.CpuConfig
import config.CpuConfig._

class Decode extends Module{
  val io = IO(new Bundle {
    val fromIf        = Input(new IfIdBundle)
    val writeBackAddr = Input(UInt(regAddrWidth.W))
    val writeBackData = Input(UInt(dataWidth.W))
    val writeBackEn   = Input(Bool())
    val toEx          = Output(new IdExBundle)
    val branchTaken   = Output(Bool())
    val branchAddr    = Output(UInt(addrWidth.W))

    // DEBUG
    val regfileDebug = Output(Vec(regAmount, UInt(dataWidth.W)))
    // DEBUG
  })


  val comparator = Module(new Comparator())
  val controller = Module(new Controller())
  val regFile    = Module(new RegFile())

  // DEBUG
  io.regfileDebug := regFile.io.debug
  // DEBUG

  // Controller
  controller.io.instruction := io.fromIf.instruction
  io.toEx.control.regWriteEn := controller.io.regWriteEn
  io.toEx.control.memToReg := controller.io.memToReg
  io.toEx.control.storeMode := controller.io.storeMode
  io.toEx.control.aluOp := controller.io.aluOp
  io.toEx.control.aluXFromShamt := controller.io.aluXFromShamt
  io.toEx.control.aluYFromImm := controller.io.aluYFromImm

  // RegFile
  val rsData = Wire(UInt(dataWidth.W))
  val rtData = Wire(UInt(dataWidth.W))
  regFile.io.r1Addr := io.fromIf.instruction(25, 21)  // rs
  regFile.io.r2Addr := io.fromIf.instruction(20, 16)  // rt
  rsData := MuxLookup(
    key = controller.io.rsrtRecipe,
    default = regFile.io.r1Data,
    mapping = Seq(
      RsRtRecipe.normal -> regFile.io.r1Data,
      RsRtRecipe.link   -> io.fromIf.pcplusfour,
      RsRtRecipe.lui    -> 0.U(dataWidth.W)
    )
  )
  io.toEx.rs := rsData
  rtData := MuxLookup(
    key = controller.io.rsrtRecipe,
    default = regFile.io.r2Data,
    mapping = Seq(
      RsRtRecipe.normal -> regFile.io.r2Data,
      RsRtRecipe.link   -> 4.U(dataWidth.W),
      RsRtRecipe.lui    -> 0.U(dataWidth.W)
    )
  )
  io.toEx.rt := rtData
  io.toEx.writeRegAddr := MuxLookup(
    key = controller.io.regDst,
    default = io.fromIf.instruction(15, 11),
    mapping = Seq(
      RegDst.rt    -> io.fromIf.instruction(20, 16),
      RegDst.rd    -> io.fromIf.instruction(15, 11),
      RegDst.GPR31 -> 31.U(regAddrWidth.W)
    )
  )
  regFile.io.writeEnable := io.writeBackEn
  regFile.io.writeAddr := io.writeBackAddr
  regFile.io.writeData := io.writeBackData

  val extendedImm = Wire(UInt(dataWidth.W))
  extendedImm := MuxLookup(
    key = controller.io.immRecipe,
    default = 0.U(dataWidth.W),
    mapping = Seq(
      ImmRecipe.sExt -> io.fromIf.instruction(15, 0),
      ImmRecipe.uExt -> Cat(0.U(15.W), io.fromIf.instruction(15, 0)),
      ImmRecipe.lui  -> Cat(io.fromIf.instruction(15, 0), 0.U(15.W))
    )
  )
  io.toEx.immediate := extendedImm
  io.branchAddr := MuxLookup(
    key = controller.io.jCond,
    default = Cat(extendedImm(29, 0), 0.U(2.W)) + io.fromIf.pcplusfour,
    mapping = Seq(
      JCond.j  -> Cat(io.fromIf.pcplusfour(31,28), io.fromIf.instruction(25,0), 0.U(2.W)),
      JCond.jr -> rsData,
      JCond.b  -> (Cat(extendedImm(29, 0), 0.U(2.W)) + io.fromIf.pcplusfour)
    )
  )

  // Comparator, 分支判断
  comparator.io.x := rsData
  comparator.io.y := rtData
  io.branchTaken := MuxLookup(
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

  // Shamt
  io.toEx.shamt := io.fromIf.instruction(10, 6)
}

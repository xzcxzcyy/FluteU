package flute.core.decode

import Chisel.Cat
import chisel3._
import chisel3.util.MuxLookup
import flute.config.CPUConfig._
import flute.core.components._
import flute.core.fetch.IBEntry

class MicroOp extends Bundle {
  class ControlSig extends Bundle {
    val regWriteEn    = Bool()
    val loadMode      = Bool()
    val storeMode     = UInt(StoreMode.width.W)
    val aluOp         = UInt(ALUOp.width.W)
    val aluYFromImm   = Bool()
    val aluXFromShamt = Bool()
    val bjCond        = UInt(BJCond.width.W)
  }
  val controlSig   = new ControlSig()
  val rs           = UInt(dataWidth.W)
  val rt           = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
  val immediate    = UInt(dataWidth.W)
  val shamt        = UInt(shamtWidth.W)

  // for issue wake up
  val rsAddr = UInt(instrWidth.W)
  val rtAddr = UInt(instrWidth.W)
  // temporary: branchAddr calculated in Ex
  val pc     = UInt(instrWidth.W)
}

class Decoder extends Module {
  val io = IO(new Bundle {
    val instr       = Input(new IBEntry)
    val withRegfile = Flipped(new RegFileReadIO)
    val microOp     = Output(new MicroOp)
  })

  // 解开 Fetch 传来的 IBEntry 结构
  val instruction = Wire(UInt(instrWidth.W))
  instruction   := io.instr.inst
  io.microOp.pc := io.instr.addr

  // Controller //////////////////////////////////////////////////////
  val controller = Module(new Controller())
  controller.io.instr                 := io.instr.inst
  io.microOp.controlSig.regWriteEn    := controller.io.regWriteEn
  io.microOp.controlSig.loadMode      := controller.io.memToReg
  io.microOp.controlSig.storeMode     := controller.io.storeMode
  io.microOp.controlSig.aluOp         := controller.io.aluOp
  io.microOp.controlSig.aluXFromShamt := controller.io.aluXFromShamt
  io.microOp.controlSig.aluYFromImm   := controller.io.aluYFromImm
  io.microOp.controlSig.bjCond        := controller.io.bjCond
  ////////////////////////////////////////////////////////////////////

  // RegFile /////////////////////////////////////////////////////////

  val rsDataWire = Wire(UInt(dataWidth.W))
  val rtDataWire = Wire(UInt(dataWidth.W))
  io.withRegfile.r1Addr := instruction(25, 21)
  io.withRegfile.r2Addr := instruction(20, 16)
  rsDataWire            := io.withRegfile.r1Data
  io.microOp.rs         := rsDataWire
  rtDataWire            := io.withRegfile.r2Data
  io.microOp.rt         := rtDataWire
  io.microOp.writeRegAddr := MuxLookup(
    key = controller.io.regDst,
    default = instruction(15, 11),
    mapping = Seq(
      RegDst.rt    -> instruction(20, 16),
      RegDst.rd    -> instruction(15, 11),
      RegDst.GPR31 -> 31.U(regAddrWidth.W)
    )
  )
  /////////////////////////////////////////////////////////////////

  // Immediate ////////////////////////////////////////////////////
  val extendedImm = Wire(UInt(dataWidth.W))
  extendedImm := MuxLookup(
    key = controller.io.immRecipe,
    default = 0.U(dataWidth.W),
    mapping = Seq(
      ImmRecipe.sExt -> instruction(15, 0),
      ImmRecipe.uExt -> Cat(0.U(15.W), instruction(15, 0)),
      ImmRecipe.lui  -> Cat(instruction(15, 0), 0.U(15.W))
    )
  )
  io.microOp.immediate := extendedImm
  /////////////////////////////////////////////////////////////////

  // Shamt ////////////////////////////////////////////////////////
  io.microOp.shamt := instruction(10, 6)
  /////////////////////////////////////////////////////////////////

  // Issue Wake Up ////////////////////////////////////////////////
  io.microOp.rsAddr := instruction(25, 21)
  io.microOp.rtAddr := instruction(20, 16)
}

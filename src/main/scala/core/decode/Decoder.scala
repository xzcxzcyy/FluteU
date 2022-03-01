package core.decode

import Chisel.Cat
import chisel3._
import chisel3.util.MuxLookup
import config.CPUConfig._
import core.components._

class MicroOp extends Bundle {
  class ControlSig extends Bundle {
    val regWriteEn    = Bool()
    val loadMode      = Bool()
    val storeMode     = UInt(StoreMode.width.W)
    val aluOp         = UInt(ALUOp.width.W)
    val aluYFromImm   = Bool()
    val aluXFromShamt = Bool()
  }
  val controlSig      = new ControlSig()
  val rs           = UInt(dataWidth.W)
  val rt           = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
  val immediate    = UInt(dataWidth.W)
  val shamt        = UInt(shamtWidth.W)
}

class Decoder extends Module {
  val io = IO(new Bundle {
    val instr         = Input(UInt(instrWidth.W))
    val withRegfile   = Flipped(new RegFileReadIO)
    val microOp       = Output(new MicroOp)
  })

  // Controller //////////////////////////////////////////////////////
  val controller = Module(new Controller())
  controller.io.instr := io.instr
  io.microOp.controlSig.regWriteEn    := controller.io.regWriteEn
  io.microOp.controlSig.loadMode      := controller.io.memToReg
  io.microOp.controlSig.storeMode     := controller.io.storeMode
  io.microOp.controlSig.aluOp         := controller.io.aluOp
  io.microOp.controlSig.aluXFromShamt := controller.io.aluXFromShamt
  io.microOp.controlSig.aluYFromImm   := controller.io.aluYFromImm
  ////////////////////////////////////////////////////////////////////

  // RegFile /////////////////////////////////////////////////////////

  val rsDataWire = Wire(UInt(dataWidth.W))
  val rtDataWire = Wire(UInt(dataWidth.W))
  io.withRegfile.r1Addr := io.instr(25, 21)
  io.withRegfile.r2Addr := io.instr(20, 16)
  rsDataWire := io.withRegfile.r1Data
  io.microOp.rs := rsDataWire
  rtDataWire := io.withRegfile.r2Data
  io.microOp.rt := rtDataWire
  io.microOp.writeRegAddr := MuxLookup(
    key = controller.io.regDst,
    default = io.instr(15, 11),
    mapping = Seq(
      RegDst.rt    -> io.instr(20, 16),
      RegDst.rd    -> io.instr(15, 11),
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
      ImmRecipe.sExt -> io.instr(15, 0),
      ImmRecipe.uExt -> Cat(0.U(15.W), io.instr(15, 0)),
      ImmRecipe.lui  -> Cat(io.instr(15, 0), 0.U(15.W))
    )
  )
  io.microOp.immediate := extendedImm
  /////////////////////////////////////////////////////////////////

  // Shamt ////////////////////////////////////////////////////////
  io.microOp.shamt := io.instr(10, 6)
  /////////////////////////////////////////////////////////////////
}


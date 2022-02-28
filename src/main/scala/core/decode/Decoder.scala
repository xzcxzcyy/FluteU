package core.decode

import Chisel.Cat
import chisel3._
import chisel3.util.MuxLookup
import config.CPUConfig._
import core.components._

class DecodedSig extends Bundle {
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
    val writeBackAddr = Input(UInt(regAddrWidth.W))
    val writeBackData = Input(UInt(dataWidth.W))
    val writeBackEn   = Input(Bool())
    val decodedSig    = Output(new DecodedSig)
  })

  val controller = Module(new Controller())
  val regFile    = Module(new RegFile())

  // Controller //////////////////////////////////////////////////////
  controller.io.instr := io.instr
  io.decodedSig.controlSig.regWriteEn    := controller.io.regWriteEn
  io.decodedSig.controlSig.loadMode      := controller.io.memToReg
  io.decodedSig.controlSig.storeMode     := controller.io.storeMode
  io.decodedSig.controlSig.aluOp         := controller.io.aluOp
  io.decodedSig.controlSig.aluXFromShamt := controller.io.aluXFromShamt
  io.decodedSig.controlSig.aluYFromImm   := controller.io.aluYFromImm
  ////////////////////////////////////////////////////////////////////

  // RegFile /////////////////////////////////////////////////////////
  val rsData = Wire(UInt(dataWidth.W))
  val rtData = Wire(UInt(dataWidth.W))
  regFile.io.r1Addr := io.instr(25, 21)  // rs
  regFile.io.r2Addr := io.instr(20, 16)  // rt
  rsData := regFile.io.r1Data
  io.decodedSig.rs := rsData
  rtData := regFile.io.r2Data
  io.decodedSig.rt := rtData
  io.decodedSig.writeRegAddr := MuxLookup(
    key = controller.io.regDst,
    default = io.instr(15, 11),
    mapping = Seq(
      RegDst.rt    -> io.instr(20, 16),
      RegDst.rd    -> io.instr(15, 11),
      RegDst.GPR31 -> 31.U(regAddrWidth.W)
    )
  )
  regFile.io.writeEnable := io.writeBackEn
  regFile.io.writeAddr := io.writeBackAddr
  regFile.io.writeData := io.writeBackData
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
  io.decodedSig.immediate := extendedImm
  /////////////////////////////////////////////////////////////////

  // Shamt ////////////////////////////////////////////////////////
  io.decodedSig.shamt := io.instr(10, 6)
  /////////////////////////////////////////////////////////////////
}


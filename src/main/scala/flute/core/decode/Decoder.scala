package flute.core.decode

import Chisel.Cat
import chisel3._
import chisel3.util.MuxLookup
import flute.config.CPUConfig._
import flute.core.components._
import flute.core.fetch.IBEntry
import chisel3.util.Fill

class OpBundle extends Bundle {
  val op    = UInt(dataWidth.W)
  val valid = Bool()
}

class MicroOp extends Bundle {
  val regWriteEn = Bool()
  val loadMode   = UInt(LoadMode.width.W)
  val storeMode  = UInt(StoreMode.width.W)
  val aluOp      = UInt(ALUOp.width.W)
  val op1        = new OpBundle()
  val op2        = new OpBundle()
  val bjCond     = UInt(BJCond.width.W)
  val instrType  = UInt(InstrType.width.W)
  val delay      = UInt(delayWidth.W)

  val writeRegAddr = UInt(regAddrWidth.W)
  val immediate    = UInt(dataWidth.W)

  // for issue wake up
  val rsAddr = UInt(regAddrWidth.W)
  val rtAddr = UInt(regAddrWidth.W)
  // calculate branchAddr in Ex
  val pc = UInt(instrWidth.W)
}


class Decoder extends Module {
  val io = IO(new Bundle {
    val instr   = Input(new IBEntry)
    // 气泡流水 decoder不读regfile
    // val withRegfile = Flipped(new RegFileReadIO)
    val microOp = Output(new MicroOp)
  })
  val controller = Module(new Controller())

  // 解开 Fetch 传来的 IBEntry 结构
  val instruction = Wire(UInt(instrWidth.W))
  instruction   := io.instr.inst
  io.microOp.pc := io.instr.addr

  // Immediate ////////////////////////////////////////////////////
  val extendedImm = WireInit(0.U(dataWidth.W))
  extendedImm := MuxLookup(
    key = controller.io.immRecipe,
    default = 0.U(dataWidth.W),
    mapping = Seq(
      ImmRecipe.sExt -> Cat(Fill(16, instruction(15)), instruction(15, 0)),
      ImmRecipe.uExt -> Cat(0.U(16.W), instruction(15, 0)),
      ImmRecipe.lui  -> Cat(instruction(15, 0), 0.U(16.W))
    )
  )
  io.microOp.immediate := extendedImm
  /////////////////////////////////////////////////////////////////

  // Controller //////////////////////////////////////////////////////
  controller.io.instruction := io.instr.inst
  io.microOp.regWriteEn := controller.io.regWriteEn
  io.microOp.loadMode   := controller.io.loadMode
  io.microOp.storeMode  := controller.io.storeMode
  io.microOp.aluOp      := controller.io.aluOp
  io.microOp.op1.op     := MuxLookup(
    key = controller.io.op1Recipe,
    default = 0.U,
    mapping = Seq(
      Op1Recipe.rs      -> 0.U,
      Op1Recipe.pcPlus8 -> (io.instr.addr + 8.U),
      Op1Recipe.shamt   -> instruction(10, 6),
      Op1Recipe.zero    -> 0.U
    )
  )
  io.microOp.op1.valid  := Mux(controller.io.op1Recipe === Op1Recipe.rs, 0.B, 1.B)
  io.microOp.op2.op     := MuxLookup(
    key = controller.io.op2Recipe,
    default = 0.U,
    mapping = Seq(
      Op2Recipe.rt      -> 0.U,
      Op2Recipe.imm     -> extendedImm,
      Op2Recipe.zero    -> 0.U
    )
  )
  io.microOp.op2.valid := Mux(controller.io.op2Recipe === Op2Recipe.rt, 0.B, 1.B)
  io.microOp.bjCond    := controller.io.bjCond
  io.microOp.instrType := controller.io.instrType
  io.microOp.delay     := controller.io.delay
  ////////////////////////////////////////////////////////////////////

  // RegFile /////////////////////////////////////////////////////////
  io.microOp.writeRegAddr := MuxLookup(
    key = controller.io.regDst,
    default = instruction(15, 11),
    mapping = Seq(
      RegDst.rt    -> instruction(20, 16),
      RegDst.rd    -> instruction(15, 11),
      RegDst.GPR31 -> 31.U(regAddrWidth.W)
    )
  )
  // Issue Wake Up
  io.microOp.rsAddr := instruction(25, 21)
  io.microOp.rtAddr := instruction(20, 16)
  /////////////////////////////////////////////////////////////////

}

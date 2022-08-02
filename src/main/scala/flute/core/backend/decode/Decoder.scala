package flute.core.backend.decode

import Chisel.Cat
import chisel3._
import chisel3.util.MuxLookup
import flute.config.CPUConfig._
import flute.core.components._
import flute.core.frontend.IBEntry
import chisel3.util.Fill

class OpBundle extends Bundle {
  val op    = UInt(dataWidth.W)
  val valid = Bool()
}

class MicroOp(rename: Boolean = false) extends Bundle {
  private val regWidth = if (rename) phyRegAddrWidth else regAddrWidth

  val regWriteEn = Bool()
  val loadMode   = UInt(LoadMode.width.W)
  val storeMode  = UInt(StoreMode.width.W)
  val aluOp      = UInt(ALUOp.width.W)
  val mduOp      = UInt(MDUOp.width.W)
  val op1        = new OpBundle()
  val op2        = new OpBundle()
  val bjCond     = UInt(BJCond.width.W)
  val instrType  = UInt(InstrType.width.W)

  val writeRegAddr = UInt(regWidth.W)
  val immediate    = UInt(dataWidth.W)

  // for issue wake up
  val rsAddr = UInt(regWidth.W)
  val rtAddr = UInt(regWidth.W)
  // calculate branchAddr in Ex
  val pc      = UInt(instrWidth.W)
  val robAddr = UInt(robEntryNumWidth.W)

  val predictBT = UInt(addrWidth.W)
  val inSlot    = Bool()

  val cp0RegAddr = UInt(5.W)
  val cp0RegSel  = UInt(3.W)
}

class Decoder extends Module {
  val io = IO(new Bundle {
    val instr = Input(new IBEntry)
    // 气泡流水 decoder不读regfile
    // val withRegfile = Flipped(new RegFileReadIO)
    val microOp = Output(new MicroOp)
  })
  val controller = Module(new Controller)

  // 解开 Fetch 传来的 IBEntry 结构
  val instruction = Wire(UInt(instrWidth.W))
  instruction   := io.instr.inst
  io.microOp.pc := io.instr.addr

  io.microOp.predictBT := io.instr.predictBT
  io.microOp.inSlot    := io.instr.inSlot

  // Immediate ////////////////////////////////////////////////////
  val extendedImm = WireInit(0.U(dataWidth.W))
  extendedImm := MuxLookup(
    key = controller.io.immRecipe,
    default = 0.U(dataWidth.W),
    mapping = Seq(
      ImmRecipe.sExt -> Cat(Fill(16, instruction(15)), instruction(15, 0)),
      ImmRecipe.uExt -> Cat(0.U(16.W), instruction(15, 0)),
      ImmRecipe.lui  -> Cat(instruction(15, 0), 0.U(16.W)),
    )
  )
  io.microOp.immediate := extendedImm
  /////////////////////////////////////////////////////////////////

  // Controller //////////////////////////////////////////////////////
  controller.io.instruction := io.instr.inst
  val writeArfRegAddr = MuxLookup(
    key = controller.io.regDst,
    default = instruction(15, 11),
    mapping = Seq(
      RegDst.rt    -> instruction(20, 16),
      RegDst.rd    -> instruction(15, 11),
      RegDst.GPR31 -> 31.U(regAddrWidth.W)
    )
  )
  io.microOp.regWriteEn := controller.io.regWriteEn && writeArfRegAddr =/= 0.U
  io.microOp.loadMode   := controller.io.loadMode
  io.microOp.storeMode  := controller.io.storeMode
  io.microOp.aluOp      := controller.io.aluOp
  io.microOp.mduOp      := controller.io.mduOp
  io.microOp.op1.op := MuxLookup(
    key = controller.io.op1Recipe,
    default = 0.U,
    mapping = Seq(
      Op1Recipe.rs      -> 0.U,
      Op1Recipe.pcPlus8 -> (io.instr.addr + 8.U),
      Op1Recipe.shamt   -> instruction(10, 6),
      Op1Recipe.zero    -> 0.U
    )
  )
  io.microOp.op1.valid := Mux(controller.io.op1Recipe === Op1Recipe.rs, 0.B, 1.B)
  io.microOp.op2.op := MuxLookup(
    key = controller.io.op2Recipe,
    default = 0.U,
    mapping = Seq(
      Op2Recipe.rt   -> 0.U,
      Op2Recipe.imm  -> extendedImm,
      Op2Recipe.zero -> 0.U
    )
  )
  io.microOp.op2.valid := Mux(controller.io.op2Recipe === Op2Recipe.rt, 0.B, 1.B)
  io.microOp.bjCond    := controller.io.bjCond
  io.microOp.instrType := controller.io.instrType
  ////////////////////////////////////////////////////////////////////

  // RegFile /////////////////////////////////////////////////////////
  io.microOp.writeRegAddr := writeArfRegAddr
  // Issue Wake Up
  io.microOp.rsAddr := instruction(25, 21)
  io.microOp.rtAddr := instruction(20, 16)
  /////////////////////////////////////////////////////////////////

  io.microOp.robAddr := DontCare

  val cp0RegAddr = instruction(15, 11)
  val cp0RegSel  = instruction(2, 0)

  io.microOp.cp0RegAddr := cp0RegAddr
  io.microOp.cp0RegSel  := cp0RegSel
}

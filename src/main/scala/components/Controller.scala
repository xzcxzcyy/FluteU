package components

import chisel3._
import chisel3.util.BitPat
import chisel3.util.ListLookup
import config.CpuConfig._



class Controller extends Module {
  val io = IO(new Bundle {
    val instruction   = Input(UInt(instrWidth.W))

    val regWriteEn    = Output(Bool())
    val memToReg      = Output(Bool())
    val storeMode     = Output(UInt(storeModeWidth.W))
    val aluOp         = Output(UInt(aluOpWidth.W))
    val aluXFromShamt = Output(Bool())
    val aluYFromImm   = Output(Bool())

    val branchCond    = Output(UInt(branchCondWidth.W))
    val jCond         = Output(UInt(jCondWidth.W))
    val regDst        = Output(Bool())
  })

  // @formatter:off
  val signals = ListLookup(io.instruction,
    // regWriteEn, memToReg, storeMode,          aluOp,  aluXFromShamt,aluYFromImm, branchCond,   jCond,     regDst
    /* default */
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B,  BranchCond.none, JCond.j,   RegDst.rd),
    Array(
    /** Logical Instructions **/
    /* AND */ BitPat("b000000???????????????00000100100") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.and,  false.B,  false.B,  BranchCond.none, JCond.j,   RegDst.rd),
    /* OR */ BitPat("b000000???????????????00000100101") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  false.B,  BranchCond.none, JCond.j,   RegDst.rd),
    /* XOR */ BitPat("b000000???????????????00000100110") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.xor,  false.B,  false.B,  BranchCond.none, JCond.j,   RegDst.rd),
    /* NOR */ BitPat("b000000???????????????00000100111") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.nor,  false.B,  false.B,  BranchCond.none, JCond.j,   RegDst.rd),
    /* ANDI */ BitPat("b001100??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.and,  false.B,  true.B,   BranchCond.none, JCond.j,   RegDst.rt),
    /* ORI */ BitPat("b001101??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  true.B,   BranchCond.none, JCond.j,   RegDst.rt),
    /* XORI */ BitPat("b001110??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.xor,  false.B,  true.B,   BranchCond.none, JCond.j,   RegDst.rt),
    /* LUI */ BitPat("b00111100000?????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.nor,  true.B,   true.B,   BranchCond.none, JCond.j,   RegDst.rt),
    )
  )
}

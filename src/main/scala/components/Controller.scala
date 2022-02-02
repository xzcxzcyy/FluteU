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
    val rsrtRecipe    = Output(UInt(rsrtRecipeWidth.W))
    val immRecipe     = Output(UInt(immRecipeWidth.W))
  })

  // @formatter:off
  val signals = ListLookup(io.instruction,
    // regWriteEn, memToReg, storeMode,          aluOp,  aluXFromShamt,aluYFromImm, branchCond,   jCond,    regDst       RsRtRecipe         ImmRecipe
    /* default */
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    Array(
    /** Logical Instructions **/
    /* AND */ BitPat("b000000???????????????00000100100") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.and,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* OR */ BitPat("b000000???????????????00000100101") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* XOR */ BitPat("b000000???????????????00000100110") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.xor,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* NOR */ BitPat("b000000???????????????00000100111") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.nor,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* ANDI */ BitPat("b001100??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.and,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* ORI */ BitPat("b001101??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* XORI */ BitPat("b001110??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.xor,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* LUI */ BitPat("b00111100000?????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.lui,    ImmRecipe.lui),
    /** Arithmetic Instructions **/
    /* ADD */ BitPat("b000000???????????????00000100000") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.add,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* ADDI */ BitPat("b001000??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.add,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* ADDU */ BitPat("b000000???????????????00000100001") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.addu, false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* ADDIU */ BitPat("b001001??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.addu, false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* SUB */ BitPat("b000000???????????????00000100010") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.sub,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* SUBU */ BitPat("b000000???????????????00000100011") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.subu, false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /** Branch and Jump Instructions **/
    /* BEQ,B */ BitPat("b000100??????????????????????????") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.eq,   JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* BGEZ */ BitPat("b000001?????00001????????????????") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.gez,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* BGEZAL,BAL */ BitPat("b000001?????10001????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.gez,  JCond.b,  RegDst.GPR31, RsRtRecipe.link,   ImmRecipe.sExt),
    /* BGTZ */ BitPat("b000111?????00000????????????????") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.gtz,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* BLEZ */ BitPat("b000110?????00000????????????????") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.lez,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* BLTZ */ BitPat("b000001?????00000????????????????") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.ltz,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* BLTZAL */ BitPat("b000001?????10000????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.ltz,  JCond.b,  RegDst.GPR31, RsRtRecipe.link,   ImmRecipe.sExt),
    /* BNE */ BitPat("b000101??????????????????????????") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.ne,   JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* J */ BitPat("b000010??????????????????????????") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* JAL */ BitPat("b000011??????????????????????????") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.j,  RegDst.GPR31, RsRtRecipe.link,   ImmRecipe.sExt),
    /* JALR */ BitPat("b000000?????00000??????????001001") ->
      List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.jr, RegDst.rd,    RsRtRecipe.link,   ImmRecipe.sExt),
    /* JR */ BitPat("b000000?????0000000000?????001000") ->
      List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.jr, RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /** Load, Store, and Memory Control Instructions **/
    /* LB */
    /* LBU */
    /* LH */
    /* LHU */
    /* LL */
    /* LW */
    /* LWL */
    /* LWR */
    /* PREF */
    /* SB */
    /* SC */
    /* SD */
    /* SW */
    /* SWL */
    /* SWR */
    /* SYNC */
    )
  )
}

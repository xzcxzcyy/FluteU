package core.components

import chisel3._
import chisel3.util.ListLookup
import config.CpuConfig._
import config.Instructions._

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
    /*default*/
              List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    Array(
    /** Logical Instructions **/
    AND    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.and,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    OR     -> List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    XOR    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.xor,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    NOR    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.nor,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    ANDI   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.and,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    ORI    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    XORI   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.xor,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    LUI    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.or,   false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.lui,    ImmRecipe.lui),
    /** Arithmetic Instructions **/
    ADD    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.add,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    ADDI   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.add,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    ADDU   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.addu, false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    ADDIU  -> List(true.B,  false.B, StoreMode.disable,  ALUOp.addu, false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    SUB    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.sub,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    SUBU   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.subu, false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /** Branch and Jump Instructions **/
    BEQ    -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.eq,   JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    BGEZ   -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.gez,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    BGEZAL -> List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.gez,  JCond.b,  RegDst.GPR31, RsRtRecipe.link,   ImmRecipe.sExt),
    BGTZ   -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.gtz,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    BLEZ   -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.lez,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    BLTZ   -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.ltz,  JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    BLTZAL -> List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.ltz,  JCond.b,  RegDst.GPR31, RsRtRecipe.link,   ImmRecipe.sExt),
    BNE    -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.ne,   JCond.b,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    J      -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    JAL    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.j,  RegDst.GPR31, RsRtRecipe.link,   ImmRecipe.sExt),
    JALR   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.jr, RegDst.rd,    RsRtRecipe.link,   ImmRecipe.sExt),
    JR     -> List(false.B, false.B, StoreMode.disable,  ALUOp.none, false.B,  false.B, BranchCond.all,  JCond.jr, RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /** Load, Store, and Memory Control Instructions **/
    LB     -> List(true.B,  true.B,  StoreMode.disable,  ALUOp.add,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
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
    /** Move Instructions **/
    /** Shift Instructions **/
    /* SLL */
    /* SLLV */
    /* SRA */
    /* SRAV */
    /* SRL */
    /* SRLV */
    /** Trap Instructions **/
    /** No Operation **/
    /* NOP */
    )
  )

  io.regWriteEn := signals(0)
  io.memToReg   := signals(1)
  io.storeMode  := signals(2)
  io.aluOp      := signals(3)
  io.aluXFromShamt := signals(4)
  io.aluYFromImm   := signals(5)
  io.branchCond    := signals(6)
  io.jCond         := signals(7)
  io.regDst        := signals(8)
  io.rsrtRecipe    := signals(9)
  io.immRecipe     := signals(10)
}

object StoreMode {
  val disable  = 0.U(storeModeWidth.W)
  val word     = 1.U(storeModeWidth.W)
  val byte     = 2.U(storeModeWidth.W)
  val halfword = 3.U(storeModeWidth.W)
}

object BranchCond {
  val none = 0.U(branchCondWidth.W)
  val eq   = 1.U(branchCondWidth.W)
  val ge   = 2.U(branchCondWidth.W)
  val gez  = 3.U(branchCondWidth.W)
  val geu  = 4.U(branchCondWidth.W)
  val gt   = 5.U(branchCondWidth.W)
  val gtz  = 6.U(branchCondWidth.W)
  val gtu  = 7.U(branchCondWidth.W)
  val le   = 8.U(branchCondWidth.W)
  val lez  = 9.U(branchCondWidth.W)
  val leu  = 10.U(branchCondWidth.W)
  val lt   = 11.U(branchCondWidth.W)
  val ltz  = 12.U(branchCondWidth.W)
  val ltu  = 13.U(branchCondWidth.W)
  val ne   = 14.U(branchCondWidth.W)
  val all  = 15.U(branchCondWidth.W)
}

object JCond {
  val j  = 0.U(jCondWidth.W)
  val jr = 1.U(jCondWidth.W)
  val b  = 2.U(jCondWidth.W)
}

object RegDst {
  val rt    = 0.U(regDstWidth.W)
  val rd    = 1.U(regDstWidth.W)
  val GPR31 = 2.U(regDstWidth.W)
}

object RsRtRecipe {
  val normal = 0.U(rsrtRecipeWidth.W)
  val link   = 1.U(rsrtRecipeWidth.W)
  val lui    = 2.U(rsrtRecipeWidth.W)
}

object ImmRecipe {
  val sExt = 0.U(immRecipeWidth.W)
  val uExt = 1.U(immRecipeWidth.W)
  val lui  = 2.U(immRecipeWidth.W)
}

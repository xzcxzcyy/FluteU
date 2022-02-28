package core.decode

import chisel3._
import chisel3.util.{ListLookup, log2Up}
import config.CPUConfig._
import config.Instructions._
import core.components.ALUOp

class Controller extends Module {
  val io = IO(new Bundle {
    val instr         = Input(UInt(instrWidth.W))

    val regWriteEn    = Output(Bool())
    val memToReg      = Output(Bool())
    val storeMode     = Output(UInt(StoreMode.width.W))
    val aluOp         = Output(UInt(ALUOp.width.W))
    val aluXFromShamt = Output(Bool())
    val aluYFromImm   = Output(Bool())

    val branchCond = Output(UInt(BranchCond.width.W))
    val jCond      = Output(UInt(JCond.width.W))
    val regDst     = Output(UInt(RegDst.width.W))
    val rsrtRecipe = Output(UInt(RsRtRecipe.width.W))
    val immRecipe  = Output(UInt(ImmRecipe.width.W))
  })

  // @formatter:off
  val signals = ListLookup(io.instr,
            // regWriteEn, memToReg, storeMode,          aluOp,  aluXFromShamt,aluYFromImm, branchCond,   jCond,    regDst       rsrtRecipe         immRecipe
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
    SLT    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.slt,  false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    SLTI   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.slt,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    SLTU   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.sltu, false.B,  false.B, BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    SLTIU  -> List(true.B,  false.B, StoreMode.disable,  ALUOp.sltu, false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
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
    /* LB */
    /* LBU */
    /* LH */
    /* LHU */
    /* LL */
    LW     -> List(true.B,  true.B,  StoreMode.disable,  ALUOp.add,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rt,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* LWL */
    /* LWR */
    /* PREF */
    /* SB */
    /* SC */
    /* SD */
    SW     -> List(false.B, false.B, StoreMode.word,     ALUOp.add,  false.B,  true.B,  BranchCond.none, JCond.j,  RegDst.rd,    RsRtRecipe.normal, ImmRecipe.sExt),
    /* SWL */
    /* SWR */
    /* SYNC */
    /** Move Instructions **/
    /* MFHI */
    /* MFLO */
    /* MOVN */
    /* MOVZ */
    /* MTHI */
    /* MYLO */
    /** Shift Instructions **/
    SLL    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.sll,  true.B,  false.B,  BranchCond.none, JCond.j,  RegDst.rd,   RsRtRecipe.normal, ImmRecipe.sExt),
    SLLV   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.sll,  false.B, false.B,  BranchCond.none, JCond.j,  RegDst.rd,   RsRtRecipe.normal, ImmRecipe.sExt),
    SRA    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.sra,  true.B,  false.B,  BranchCond.none, JCond.j,  RegDst.rd,   RsRtRecipe.normal, ImmRecipe.sExt),
    SRAV   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.sra,  false.B, false.B,  BranchCond.none, JCond.j,  RegDst.rd,   RsRtRecipe.normal, ImmRecipe.sExt),
    SRL    -> List(true.B,  false.B, StoreMode.disable,  ALUOp.srl,  true.B,  false.B,  BranchCond.none, JCond.j,  RegDst.rd,   RsRtRecipe.normal, ImmRecipe.sExt),
    SRLV   -> List(true.B,  false.B, StoreMode.disable,  ALUOp.srl,  false.B, false.B,  BranchCond.none, JCond.j,  RegDst.rd,   RsRtRecipe.normal, ImmRecipe.sExt),
    /** Trap Instructions **/
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
  val width  = 2

  val disable  = 0.U(width.W)
  val word     = 1.U(width.W)
  val byte     = 2.U(width.W)
  val halfword = 3.U(width.W)
}

object BranchCond {
  val amount = 16
  val width = log2Up(amount)

  val none = 0.U(width.W)
  val eq   = 1.U(width.W)
  val ge   = 2.U(width.W)
  val gez  = 3.U(width.W)
  val geu  = 4.U(width.W)
  val gt   = 5.U(width.W)
  val gtz  = 6.U(width.W)
  val gtu  = 7.U(width.W)
  val le   = 8.U(width.W)
  val lez  = 9.U(width.W)
  val leu  = 10.U(width.W)
  val lt   = 11.U(width.W)
  val ltz  = 12.U(width.W)
  val ltu  = 13.U(width.W)
  val ne   = 14.U(width.W)
  val all  = 15.U(width.W)
}

object JCond {
  val width = 2
  val j  = 0.U(width.W)
  val jr = 1.U(width.W)
  val b  = 2.U(width.W)
}

object RegDst {
  val width = 2

  val rt    = 0.U(width.W)
  val rd    = 1.U(width.W)
  val GPR31 = 2.U(width.W)
}

object RsRtRecipe {
  val width = 2

  val normal = 0.U(width.W)
  val link   = 1.U(width.W)
  val lui    = 2.U(width.W)
}

object ImmRecipe {
  val width = 2

  val sExt = 0.U(width.W)
  val uExt = 1.U(width.W)
  val lui  = 2.U(width.W)
}

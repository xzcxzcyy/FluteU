package flute.core.decode

import chisel3._
import chisel3.util.{ListLookup, log2Up}
import flute.config.CPUConfig._
import flute.config.Instructions._
import flute.core.components.ALUOp


class Controller extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(instrWidth.W))

    val regWriteEn = Output(Bool())
    val loadMode   = Output(UInt(LoadMode.width.W))
    val storeMode  = Output(UInt(StoreMode.width.W))
    val aluOp      = Output(UInt(ALUOp.width.W))
    val op1Recipe  = Output(UInt(Op1Recipe.width.W))
    val op2Recipe  = Output(UInt(Op2Recipe.width.W))
    val bjCond     = Output(UInt(BJCond.width.W))
    val regDst     = Output(UInt(RegDst.width.W))
    val immRecipe  = Output(UInt(ImmRecipe.width.W))
  })

  // @formatter:off
  val signals = ListLookup(io.instruction,
           //   regWriteEn, loadMode,          storeMode,           aluOp,      op1Recipe,         op2Recipe,      bjCond,      regDst        immRecipe
    /*default*/
              List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    Array(
    /** Logical Instructions **/
    AND    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.and,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    OR     -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.or,   Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    XOR    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.xor,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    NOR    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.nor,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    ANDI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.and,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.uExt),
    ORI    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.or,   Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.uExt),
    XORI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.xor,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.uExt),
    LUI    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.or,   Op1Recipe.zero,    Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.lui),
    /** Arithmetic Instructions **/
    ADD    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    ADDI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt),
    ADDU   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.addu, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    ADDIU  -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.addu, Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt),
    SUB    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.sub,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SUBU   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.subu, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SLT    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.slt,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SLTI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.slt,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt),
    SLTU   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.sltu, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SLTIU  -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.sltu, Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt),
    /** Branch and Jump Instructions **/
    BEQ    -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.eq,   RegDst.rd,    ImmRecipe.sExt),
    BGEZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.gez,  RegDst.rd,    ImmRecipe.sExt),
    BGEZAL -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.gez,  RegDst.GPR31, ImmRecipe.sExt),
    BGTZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.gtz,  RegDst.rd,    ImmRecipe.sExt),
    BLEZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.lez,  RegDst.rd,    ImmRecipe.sExt),
    BLTZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.ltz,  RegDst.rd,    ImmRecipe.sExt),
    BLTZAL -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.ltz,  RegDst.GPR31, ImmRecipe.sExt),
    BNE    -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.ne,   RegDst.rd,    ImmRecipe.sExt),
    J      -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.all,  RegDst.rd,    ImmRecipe.sExt),
    JAL    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.all,  RegDst.GPR31, ImmRecipe.sExt),
    // JALR   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.jr,   RegDst.rd,    ImmRecipe.sExt),
    JR     -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.jr,   RegDst.rd,    ImmRecipe.sExt),
    /** Load, Store, and Memory Control Instructions **/
    /* LB */
    /* LBU */
    /* LH */
    /* LHU */
    /* LL */
    LW     -> List(true.B,  LoadMode.word,     StoreMode.disable,   ALUOp.add,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt),
    /* LWL */
    /* LWR */
    /* PREF */
    SB     -> List(false.B, LoadMode.disable,  StoreMode.byte,      ALUOp.none,  Op1Recipe.rs,      Op2Recipe.rt,  BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    /* SC */
    /* SD */
    SW     -> List(false.B, LoadMode.disable,  StoreMode.word,      ALUOp.none,  Op1Recipe.rs,      Op2Recipe.rt,  BJCond.none, RegDst.rd,    ImmRecipe.sExt),
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
    SLL    -> List(true.B,  LoadMode.disable, StoreMode.disable,   ALUOp.sll,   Op1Recipe.shamt,   Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SLLV   -> List(true.B,  LoadMode.disable, StoreMode.disable,   ALUOp.sll,   Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SRA    -> List(true.B,  LoadMode.disable, StoreMode.disable,   ALUOp.sra,   Op1Recipe.shamt,   Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SRAV   -> List(true.B,  LoadMode.disable, StoreMode.disable,   ALUOp.sra,   Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SRL    -> List(true.B,  LoadMode.disable, StoreMode.disable,   ALUOp.srl,   Op1Recipe.shamt,   Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    SRLV   -> List(true.B,  LoadMode.disable, StoreMode.disable,   ALUOp.srl,   Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt),
    /** Trap Instructions **/
    )
  )

  io.regWriteEn    := signals(0)
  io.loadMode      := signals(1)
  io.storeMode     := signals(2)
  io.aluOp         := signals(3)
  io.op1Recipe     := signals(4)
  io.op2Recipe     := signals(5)
  io.bjCond        := signals(6)
  io.regDst        := signals(7)
  io.immRecipe     := signals(8)
}

object LoadMode {
  val width = 2

  val disable  = 0.U(width.W)
  val word     = 1.U(width.W)
  val byte     = 2.U(width.W)
  val halfword = 3.U(width.W)
}

object StoreMode {
  val width  = 2

  val disable  = 0.U(width.W)
  val word     = 1.U(width.W)
  val byte     = 2.U(width.W)
  val halfword = 3.U(width.W)
}

// ATTENTION: Ensure Op1Recipe.width >= Op2Recipe.width (had better ==)
object Op1Recipe {
  val width = 2

  val rs      = 0.U(width.W)
  val pcPlus8 = 1.U(width.W)
  val shamt   = 2.U(width.W)
  val zero    = 3.U(width.W)
}

object Op2Recipe {
  val width = 2

  val rt   = 0.U(width.W)
  val imm  = 1.U(width.W)
  val zero = 2.U(width.W)
}

object BJCond {
  val amount = 17
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
  val jr   = 15.U(width.W)  // 包含 jr, jalr (disabled)
  val all  = 16.U(width.W)  // 包含 j, jal
}

object RegDst {
  val width = 2

  val rt    = 0.U(width.W)
  val rd    = 1.U(width.W)
  val GPR31 = 2.U(width.W)
}

object ImmRecipe {
  val width = 2

  val sExt = 0.U(width.W)
  val uExt = 1.U(width.W)
  val lui  = 2.U(width.W)
}

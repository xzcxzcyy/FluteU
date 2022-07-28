package flute.core.backend.decode

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
    val mduOp      = Output(UInt(MDUOp.width.W))
    val op1Recipe  = Output(UInt(Op1Recipe.width.W))
    val op2Recipe  = Output(UInt(Op2Recipe.width.W))
    val bjCond     = Output(UInt(BJCond.width.W))
    val regDst     = Output(UInt(RegDst.width.W))
    val immRecipe  = Output(UInt(ImmRecipe.width.W))
    val instrType  = Output(UInt(InstrType.width.W))
  })

  // @formatter:off
  val signals = ListLookup(io.instruction,
           //   regWriteEn, loadMode,          storeMode,           aluOp,      op1Recipe,         op2Recipe,      bjCond,      regDst        immRecipe       instrType,     mduOp
    /*default*/
              List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    Array(
    /** Logical Instructions **/
    AND    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.and,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    OR     -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.or,   Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    XOR    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.xor,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    NOR    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.nor,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    ANDI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.and,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.uExt, InstrType.alu, MDUOp.none),
    ORI    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.or,   Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.uExt, InstrType.alu, MDUOp.none),
    XORI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.xor,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.uExt, InstrType.alu, MDUOp.none),
    LUI    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.or,   Op1Recipe.zero,    Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.lui,  InstrType.alu, MDUOp.none),
    /** Arithmetic Instructions **/
    ADD    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    ADDI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    ADDU   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.addu, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    ADDIU  -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.addu, Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SUB    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.sub,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SUBU   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.subu, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SLT    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.slt,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SLTI   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.slt,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SLTU   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.sltu, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SLTIU  -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.sltu, Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    MULT   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mult),
    MULTU  -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.multu),
    DIV    -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.div),
    DIVU   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.divu),
    /** Branch and Jump Instructions **/
    BEQ    -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.eq,   RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BGEZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.gez,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BGEZAL -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.gez,  RegDst.GPR31, ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BGTZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.gtz,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BLEZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.lez,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BLTZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.ltz,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BLTZAL -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.ltz,  RegDst.GPR31, ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BNE    -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.ne,   RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    J      -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.all,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    JAL    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.all,  RegDst.GPR31, ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    // JALR   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.add,  Op1Recipe.pcPlus8, Op2Recipe.zero, BJCond.jr,   RegDst.rd,    ImmRecipe.sExt),
    JR     -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.jr,   RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    /** Load, Store, and Memory Control Instructions **/
    /* LB */
    /* LBU */
    /* LH */
    /* LHU */
    /* LL */
    LW     -> List(true.B,  LoadMode.word,     StoreMode.disable,   ALUOp.add,  Op1Recipe.rs,      Op2Recipe.imm,  BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    /* LWL */
    /* LWR */
    /* PREF */
    SB     -> List(false.B, LoadMode.disable,  StoreMode.byte,      ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    /* SC */
    /* SD */
    SW     -> List(false.B, LoadMode.disable,  StoreMode.word,      ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    /* SWL */
    /* SWR */
    /* SYNC */
    /** Move Instructions **/
    MFHI   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.hi,      Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mfhi),
    MFLO   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.lo,      Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mflo),
    MTHI   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mthi),
    MTLO   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mtlo),
    /** Shift Instructions **/
    SLL    -> List(true.B,  LoadMode.disable, StoreMode.disable,    ALUOp.sll,  Op1Recipe.shamt,   Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SLLV   -> List(true.B,  LoadMode.disable, StoreMode.disable,    ALUOp.sll,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SRA    -> List(true.B,  LoadMode.disable, StoreMode.disable,    ALUOp.sra,  Op1Recipe.shamt,   Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SRAV   -> List(true.B,  LoadMode.disable, StoreMode.disable,    ALUOp.sra,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SRL    -> List(true.B,  LoadMode.disable, StoreMode.disable,    ALUOp.srl,  Op1Recipe.shamt,   Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    SRLV   -> List(true.B,  LoadMode.disable, StoreMode.disable,    ALUOp.srl,  Op1Recipe.rs,      Op2Recipe.rt,   BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    /** Trap Instructions **/
    /** Syscall, currently Halt **/
    SYSCALL -> List(false.B, LoadMode.disable, StoreMode.disable,   ALUOp.none, Op1Recipe.zero,    Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
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
  io.instrType     := signals(9)
  io.mduOp         := signals(10)
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
  val width = 3

  val rs      = 0.U(width.W)
  val pcPlus8 = 1.U(width.W)
  val shamt   = 2.U(width.W)
  val zero    = 3.U(width.W)
  val hi      = 4.U(width.W)
  val lo      = 5.U(width.W)
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

object InstrType {
  val width = 2

  val alu       = 0.U(width.W)
  val mulDiv    = 1.U(width.W)
  val loadStore = 2.U(width.W)
}

object MDUOp {
  val width = 3

  val none  = 0.U(width.W)
  val mult  = 1.U(width.W)
  val multu = 2.U(width.W)
  val div   = 3.U(width.W)
  val divu  = 4.U(width.W)
  val mfhi  = 5.U(width.W)
  val mflo  = 6.U(width.W)
  val mthi  = 7.U(width.W)
  val mtlo  = 8.U(width.W)
}

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
              List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.zero,    Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.ri),
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
    BEQ    -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.beq,   RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BGEZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.bgez,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BGEZAL -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.bgezal,RegDst.GPR31, ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BGTZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.bgtz,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BLEZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.blez,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BLTZ   -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.bltz,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BLTZAL -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.bltzal,RegDst.GPR31, ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    BNE    -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.rt,   BJCond.bne,   RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    J      -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.zero,    Op2Recipe.zero, BJCond.j,     RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    JAL    -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.zero,    Op2Recipe.zero, BJCond.jal,   RegDst.GPR31, ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    JALR   -> List(true.B,  LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.jalr,  RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    JR     -> List(false.B, LoadMode.disable,  StoreMode.disable,   ALUOp.none, Op1Recipe.rs,      Op2Recipe.zero, BJCond.jr,    RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    /** Load, Store, and Memory Control Instructions **/
    LB     -> List(true.B,  LoadMode.byteS,    StoreMode.disable,   ALUOp.none,  Op1Recipe.rs,  Op2Recipe.zero,BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    LBU    -> List(true.B,  LoadMode.byteU,    StoreMode.disable,   ALUOp.none,  Op1Recipe.rs,  Op2Recipe.zero,BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    LH     -> List(true.B,  LoadMode.halfS,    StoreMode.disable,   ALUOp.none,  Op1Recipe.rs,  Op2Recipe.zero,BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    LHU    -> List(true.B,  LoadMode.halfU,    StoreMode.disable,   ALUOp.none,  Op1Recipe.rs,  Op2Recipe.zero,BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    LW     -> List(true.B,  LoadMode.word,     StoreMode.disable,   ALUOp.none,  Op1Recipe.rs,  Op2Recipe.zero,BJCond.none, RegDst.rt,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    SB     -> List(false.B, LoadMode.disable,  StoreMode.byte,      ALUOp.none,  Op1Recipe.rs,  Op2Recipe.rt,  BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    SH     -> List(false.B, LoadMode.disable,  StoreMode.halfword,  ALUOp.none,  Op1Recipe.rs,  Op2Recipe.rt,  BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    SW     -> List(false.B, LoadMode.disable,  StoreMode.word,      ALUOp.none,  Op1Recipe.rs,  Op2Recipe.rt,  BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.loadStore, MDUOp.none),
    /** Move Instructions **/
    /* MFHI */
    MFHI   -> List(true.B, LoadMode.disable, StoreMode.disable, ALUOp.none, Op1Recipe.zero, Op2Recipe.zero, BJCond.none, RegDst.rd, ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mfhi),
    /* MFLO */
    MFLO   -> List(true.B, LoadMode.disable, StoreMode.disable, ALUOp.none, Op1Recipe.zero, Op2Recipe.zero, BJCond.none, RegDst.rd, ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mflo),
    /* MTHI */
    MTHI   -> List(false.B,LoadMode.disable, StoreMode.disable, ALUOp.none, Op1Recipe.rs,   Op2Recipe.zero, BJCond.none, RegDst.rd, ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mthi),
    /* MTLO */
    MTLO   -> List(false.B,LoadMode.disable, StoreMode.disable, ALUOp.none, Op1Recipe.rs,   Op2Recipe.zero, BJCond.none, RegDst.rd, ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mtlo),
    /* MTC0 */
    MTC0   -> List(false.B,LoadMode.disable, StoreMode.disable, ALUOp.none, Op1Recipe.zero, Op2Recipe.rt,   BJCond.none, RegDst.rd, ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mtc0),
    /* MFC0 */
    MFC0   -> List(true.B, LoadMode.disable, StoreMode.disable, ALUOp.none, Op1Recipe.zero, Op2Recipe.zero, BJCond.none, RegDst.rt, ImmRecipe.sExt, InstrType.mulDiv, MDUOp.mfc0),
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
    BREAK   -> List(false.B, LoadMode.disable, StoreMode.disable,   ALUOp.none, Op1Recipe.zero,    Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
    ERET    -> List(false.B, LoadMode.disable, StoreMode.disable,   ALUOp.none, Op1Recipe.zero,    Op2Recipe.zero, BJCond.none, RegDst.rd,    ImmRecipe.sExt, InstrType.alu, MDUOp.none),
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
  private val amount = 6
  val width = log2Up(amount)

  val disable = 0.U(width.W)
  val word    = 1.U(width.W)
  val byteS   = 2.U(width.W)
  val halfS   = 3.U(width.W)
  val byteU   = 4.U(width.W)
  val halfU   = 5.U(width.W)

  def en(lm: UInt) = {
    assert(lm.getWidth == width)
    
    lm =/= disable
  }
}

object StoreMode {
  val width  = 2

  val disable  = 0.U(width.W)
  val word     = 1.U(width.W)
  val byte     = 2.U(width.W)
  val halfword = 3.U(width.W)

  def en(sm: UInt) = {
    assert(sm.getWidth == width)

    sm =/= disable
  }
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
  val amount = 13
  val width = log2Up(amount)

  val none = 0.U(width.W)
  val beq   = 1.U(width.W)
  val bgez   = 2.U(width.W)
  val bgezal  = 3.U(width.W)
  val bgtz  = 4.U(width.W)
  val blez   = 5.U(width.W)
  val bltz  = 6.U(width.W)
  val bltzal  = 7.U(width.W)
  val bne   = 8.U(width.W)
  val j  = 9.U(width.W)
  val jal  = 10.U(width.W)
  val jalr   = 11.U(width.W)
  val jr  = 12.U(width.W)
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
  private val amount = 12
  val width = log2Up(amount)

  val none  = 0.U(width.W)
  val mult  = 1.U(width.W)
  val multu = 2.U(width.W)
  val div   = 3.U(width.W)
  val divu  = 4.U(width.W)
  val mfhi  = 5.U(width.W)
  val mflo  = 6.U(width.W)
  val mthi  = 7.U(width.W)
  val mtlo  = 8.U(width.W)
  val mfc0  = 9.U(width.W)
  val mtc0  = 10.U(width.W)
  val ri    = 11.U(width.W)
}

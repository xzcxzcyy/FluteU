package core.components

import chisel3._
import config.CPUConfig._
import chisel3.util.MuxLookup


class ALU extends Module {
  class Flag extends Bundle {
    val equal = Bool()
    val lessU = Bool()
    val lessS = Bool()
    val trap  = Bool()
  }

  val io = IO(new Bundle {
    val aluOp  = Input(UInt(ALUOp.width.W))
    val x      = Input(UInt(dataWidth.W))
    val y      = Input(UInt(dataWidth.W))
    val result = Output(UInt(dataWidth.W))
    val flag   = Output(new Flag())
  })

  io.flag.equal := io.x === io.y
  io.flag.lessS := io.x.asSInt < io.y.asSInt
  io.flag.lessU := io.x.asUInt < io.y.asUInt

  val x      = io.x
  val y      = io.y
  val addRes = x + y
  val subRes = x - y
  val overflow = MuxLookup(
    key = io.aluOp,
    default = 0.B,
    mapping = Seq(
      ALUOp.add -> ((x(31) & y(31) & ~addRes(31)) | (~x(31) & ~y(31) & addRes(31))),
      ALUOp.sub -> ((x(31) & ~y(31) & ~subRes(31)) | (~x(31) & y(31) & subRes(31))),
    )
  )

  io.flag.trap := overflow

  io.result := MuxLookup(
    key = io.aluOp,
    default = 0.U,
    mapping = Seq(
      ALUOp.and  -> (x & y),
      ALUOp.or   -> (x | y),
      ALUOp.xor  -> (x ^ y),
      ALUOp.nor  -> ~(x | y),
      ALUOp.sll  -> (y << x(4, 0)),
      ALUOp.srl  -> (y >> x(4, 0)),
      ALUOp.sra  -> (y.asSInt >> x(4, 0)).asUInt,
      ALUOp.slt  -> (io.x.asSInt < io.y.asSInt).asUInt,
      ALUOp.sltu -> (io.x.asUInt < io.y.asUInt).asUInt,
      ALUOp.add  -> addRes,
      ALUOp.sub  -> subRes,
      ALUOp.addu -> addRes,
      ALUOp.subu -> subRes,
    )
  )
}

object ALUOp {
  val width = 4

  // Empty Op
  val none = 0.U(ALUOp.width.W)
  // Bitwise Ops
  val and = 1.U(ALUOp.width.W)
  val or  = 2.U(ALUOp.width.W)
  val xor = 3.U(ALUOp.width.W)
  val nor = 4.U(ALUOp.width.W)
  // Shift Ops
  /** Logical shift left: rd ← rt << shamt. Fills bits from right with zeros. Logical shift right:
    * rd ← rt >> shamt. Fills bits from left with zeros. Arithmetic shift right: If rt is
    * negative, the leading bits are filled in with ones instead of zeros: rd ← rt >> shamt.
    *
    * Mind the order of shift oprands.
    */
  val sll = 5.U(ALUOp.width.W) // shift left  logically
  val srl = 6.U(ALUOp.width.W) // shift right logically
  val sra = 7.U(ALUOp.width.W) // shift right arithmetically
  // Set Ops
  val slt  = 8.U(ALUOp.width.W) // set less than (signed)
  val sltu = 9.U(ALUOp.width.W) // set less than (unsigned)
  // Sub and Add Ops
  val add  = 10.U(ALUOp.width.W)
  val sub  = 11.U(ALUOp.width.W)
  val addu = 12.U(ALUOp.width.W)
  val subu = 13.U(ALUOp.width.W)
}
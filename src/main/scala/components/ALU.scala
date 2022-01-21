package components

import chisel3._
import config.CpuConfig._

object ALUOp {
  // Bitwise Ops
  val and = 1.U(aluOpWidth.W)
  val or  = 2.U(aluOpWidth.W)
  val xor = 3.U(aluOpWidth.W)
  val nor = 4.U(aluOpWidth.W)
  // Shift Ops
  val sll = 5.U(aluOpWidth.W) // shift logic left
  val slr = 6.U(aluOpWidth.W) // shift logic right
  val sar = 7.U(aluOpWidth.W) // shift arithmetic right
  // Set Ops
  val slt  = 8.U(aluOpWidth.W) // set less than (signed)
  val sltu = 9.U(aluOpWidth.W) // set less than (unsigned)
  // Sub and Add Ops
  val add = 10.U(aluOpWidth.W)
  val sub = 11.U(aluOpWidth.W)
}

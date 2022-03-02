package flute.util

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._

object BitPatCombine {
  private def combine(s: Seq[BitPat])(instruction: UInt) = {
    assert(instruction.getWidth == instrWidth)
    s.foldLeft(0.B)((res, bitpat) => res || (instruction === bitpat))
  }

  def apply(s: Seq[BitPat]) = {
    combine(s) _
  }
}

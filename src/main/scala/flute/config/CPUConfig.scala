package flute.config

import chisel3._
import chisel3.util.log2Up

object CPUConfig {
  /// amount ///
  val regAmount = 32 // TODO: to be refractored 
  val LogicRegAmount = 32
  val PhyRegsAmout   = 64
  // Supported branch conditions: none eq ge geu gt gtu le leu lt ltu ne
  // val branchCondAmount = 16

  val superscalar = 2
  val decodeWay   = 2

  val fetchGroupSize   = 8
  val fetchGroupWidth  = log2Up(fetchGroupSize)
  val fetchAmountWidth = fetchGroupWidth + 1

  /// width ///
  val instrWidth       = 32
  val dataWidth        = 32
  val addrWidth        = 32
  val byteWidth        = 8
  val regAddrWidth     = 5
  val opcodeWidth      = 6
  val shamtWidth       = 5
  val iTypeImmWidth    = 16
  val LogicRegIdxWidth = log2Up(LogicRegAmount)
  val PhyRegIdxWidth   = log2Up(PhyRegsAmout)
}

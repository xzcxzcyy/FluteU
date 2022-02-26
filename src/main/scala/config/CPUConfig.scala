package flute.config

import chisel3._
import chisel3.util.log2Up

case class CPUConfig (
  /// amount ///
  val regAmount:Int = 32,
  // Supported branch conditions: none eq ge geu gt gtu le leu lt ltu ne
  // val branchCondAmount = 16  

  val superscalar:Int     = 2,
  val fetchGroupSize:Int  = 8,
  val fetchGroupWidth:Int = 3,

  /// width ///
  val instrWidth:Int      = 32,
  val dataWidth:Int       = 32,
  val addrWidth:Int       = 32,
  val regAddrWidth:Int    = 5,
  val opcodeWidth:Int     = 6,
  val shamtWidth:Int      = 5,
  val iTypeImmWidth:Int   = 16
)
package flute.config

import chisel3._
import chisel3.util.log2Up

case class CPUConfig (
  /// amount ///
  val regAmount:Int = 32,
  // Supported branch conditions: none eq ge geu gt gtu le leu lt ltu ne
  // val branchCondAmount = 16  

  val superscalar:Int     = 2,

  /// width ///
  val instrWidth:Int      = 32,
  val dataWidth:Int       = 32,
  val addrWidth:Int       = 32,
  val regAddrWidth:Int    = 5,
  // val aluOpWidth      = 4
  // val branchCondWidth = log2Up(branchCondAmount)
  val opcodeWidth:Int     = 6,
  // val storeModeWidth  = 2
  val shamtWidth:Int      = 5,
  val iTypeImmWidth:Int   = 16
  // val jCondWidth      = 2
  // val regDstWidth     = 2
  // val rsrtRecipeWidth = 2
  // val immRecipeWidth  = 2
  // val idFwdWidth      = 2
  // val exFwdWidth      = 2
)
package config

import fluteutil.Math

trait WidthConfig {
  val instrWidth      = 32
  val dataWidth       = 32
  val addrWidth       = 32
  val regAddrWidth    = 5
  val aluOpWidth      = 4
  val branchCondWidth = Math.log2Up(new AmountConfig {}.branchCondAmount)
  val opcodeWidth     = 6
  val storeModeWidth  = 2
  val shamtWidth      = 5
  val iTypeImmWidth   = 16
  val jCondWidth      = 2
  val regDstWidth     = 2
  val rsrtRecipeWidth = 2
  val immRecipeWidth  = 2
}

trait AmountConfig {
  val regAmount = 32

  /** Supported branch conditions none eq ge geu gt gtu le leu lt ltu ne
    */
  val branchCondAmount = 16
}

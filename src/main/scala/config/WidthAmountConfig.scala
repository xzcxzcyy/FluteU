package config

import fluteutil.Math

trait WidthConfig {
  val dataWidth       = 32
  val addrWidth       = 32
  val regAddrWidth    = 5
  val aluOpWidth      = 4
  val branchCondWidth = Math.log2Up((new AmountConfig {}).branchCondAmount)
}

trait AmountConfig {
  val regAmount = 32

  /** Supported branch conditions none eq ge geu gt gtu le leu lt ltu ne
    */
  val branchCondAmount = 11
}

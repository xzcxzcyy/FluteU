package config

trait WidthConfig {
  val dataWidth = 32
  val addrWidth = 32
  val regAddrWidth = 5
  val aluOpWidth = 4
}

trait AmountConfig {
  val regAmount = 32
}

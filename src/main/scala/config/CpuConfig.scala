package config

import chisel3._

object CpuConfig extends WidthConfig with AmountConfig {

  object IdRedirectChoice {
    val none    = 0.U(idRedirectChoiceWidth.W)
    val fromMem = 2.U(idRedirectChoiceWidth.W)
    val fromWb  = 3.U(idRedirectChoiceWidth.W)
  }

  object RedirectExFwd {
    val none = 0.U(redirectExFwdWidth.W)

    // distance
    val one   = 1.U(redirectExFwdWidth.W)
    val two   = 2.U(redirectExFwdWidth.W)
    val three = 3.U(redirectExFwdWidth.W)
  }
}

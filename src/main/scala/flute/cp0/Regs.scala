package flute.cp0

import chisel3._
import flute.config.CPUConfig._

abstract class CP0BaseReg {
  val reg: Data
  val addr: Int
  val sel: Int
}

class CP0BadVAddr extends CP0BaseReg {
  override val reg = RegInit(0.U(dataWidth.W))
  override val addr: Int = 8
  override val sel: Int  = 0
}

class CP0Count extends CP0BaseReg {
  override val reg = RegInit(0.U(dataWidth.W))
  override val addr: Int = 9
  override val sel: Int  = 0
}

class StatusBundle extends Bundle {
  val zero_31_23 = UInt(9.W)
  val bev        = Bool()
  val zero_21_16 = UInt(6.W)
  val im         = Vec(8, Bool())
  val zero_7_2   = UInt(6.W)
  val exl        = Bool()
  val ie         = Bool()
}

class CP0Status extends CP0BaseReg {
  override val reg = RegInit({
    val bundle = WireInit(0.U.asTypeOf(new StatusBundle))
    bundle.bev := 1.B
    bundle
  })
  assert(reg.getWidth == dataWidth)
  override val addr: Int = 12
  override val sel: Int  = 0
}

class CauseBundle extends Bundle {
  val bd         = Bool()
  val ti         = Bool()
  val zero_29_16 = UInt(14.W)
  val ip         = Vec(8, Bool())
  val zero_7     = Bool()
  val excCode    = UInt(5.W)
  val zero_1_0   = UInt(2.W)
}

class CP0Cause extends CP0BaseReg {
  override val reg: CauseBundle = RegInit(0.U.asTypeOf(new CauseBundle))
  assert(reg.getWidth == dataWidth)
  override val addr: Int = 13
  override val sel: Int  = 0
}

class CP0EPC extends CP0BaseReg {
  override val reg = RegInit(0.U(dataWidth.W))
  override val addr: Int = 14
  override val sel: Int  = 0
}

class CP0Compare extends CP0BaseReg {
  override val reg = RegInit(0.U(dataWidth.W))
  
  override val addr: Int = 11
  
  override val sel: Int = 0
  
}

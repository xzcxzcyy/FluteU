package flute.cp0

import chisel3._

class ExceptionBundle extends Bundle {
  val adEL = Bool() // addr error load
  val adES = Bool() // addr error store
  val sys  = Bool() // syscall
  val bp   = Bool() // breakpoint
  val ri   = Bool() // reserved instruction
  val ov   = Bool() // overflow
}

object ExceptionCode {
  val amount = 7
  val width  = 5 // Priviledged Resource Architecture demands this

  val int  = 0x00.U(width.W)
  val adEL = 0x04.U(width.W)
  val adEs = 0x05.U(width.W)
  val sys  = 0x08.U(width.W)
  val bp   = 0x09.U(width.W)
  val ri   = 0x0a.U(width.W)
  val ov   = 0x0c.U(width.W)
}

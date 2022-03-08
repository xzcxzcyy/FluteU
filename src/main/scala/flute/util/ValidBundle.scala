package flute.util

import chisel3._

class ValidBundle[T <: Data](gen: T) extends Bundle {
  val bits  = gen
  val valid = Bool()
}

object ValidBundle {
  def apply[T <: Data](gen: T) = new ValidBundle[T](gen)
}

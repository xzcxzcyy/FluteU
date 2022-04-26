package flute.core.components

import chisel3._
import chisel3.util._

class MuxStageRegIO[+T <: Data](gen: T) extends Bundle {

  /** Inputted data to the stage regster */
  val in = Input(gen)

  /**
    * from where we load the data
    */
  val mode = Input(UInt(MuxStageRegMode.width.W))

  /** The outputted data from the internal register */
  val out = Output(gen)
}

object MuxStageRegMode {
  val width = 2

  val next  = 0.U(width.W)
  val stall = 1.U(width.W)
  val flush = 2.U(width.W)
}

object MuxStageRegIO {
  def apply[T <: Data](gen: T): MuxStageRegIO[T] = new MuxStageRegIO(gen)
}

class MuxStageReg[+T <: Data](val gen: T) extends Module {
  val io = IO(new MuxStageRegIO[T](gen))

  val reg = RegInit(0.U.asTypeOf(gen))

  io.out := reg

  val regNext = MuxLookup(
    key = io.mode,
    default = io.in,
    mapping = Seq(
      MuxStageRegMode.next  -> io.in,
      MuxStageRegMode.flush -> 0.U.asTypeOf(gen),
      MuxStageRegMode.stall -> reg,
    )
  )
  reg := regNext
}

package flute.core.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp
import flute.core.rename.BusyTableReadPort

class AluEntry extends Bundle {
  private val deqNum = 2
  val uop            = new MicroOp(rename = true)
  val awaken         = Bool()
  val sel            = UInt(log2Ceil(deqNum).W)
}
class AluIssue(detectWidth: Int) extends Module {
  val io = IO(new Bundle {
    // 接收窗口
    val detect = Input(Vec(detectWidth, Valid(new MicroOp(rename = true))))
    // data from Issue Stage Reg
    val wake = Input(Vec(2, Valid(new AluEntry)))

    val bt = Vec(2 * detectWidth, Flipped(new BusyTableReadPort))

    val issue = Output(Vec(2, Valid(UInt(detectWidth.W))))
    val out   = Output(Vec(2, Valid(new AluEntry)))
  })
}

package flute.core.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp

class AluIssue extends Module {
  val io = IO(new Bundle {

    // 接收窗口
    val win = Input(Vec(4, Valid(new MicroOp(rename = true))))
    

  })
}
package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._


class HiLoBundle extends Bundle {
  val hi = UInt(dataWidth.W)
  val lo = UInt(dataWidth.W)
}

class MDU extends Module {
  val io = IO(new Bundle {
    val op1    = Input(UInt(dataWidth.W))
    val op2    = Input(UInt(dataWidth.W))
    val signed = Input(Bool())
    val md     = Input(Bool())  // io.md===1.U : Mul

    val result = Output(new HiLoBundle)
    val error  = Output(Bool())
  })

  // Mul
  val op1 = RegNext(io.op1, 0.U(32.W))
  val op2 = RegNext(io.op2, 0.U(32.W))
  val result = Mux(io.signed, (op1.asSInt * op2.asSInt).asUInt, op1 * op2)

  // Div
  val div = Module(new DIVBlackBox())
  div.io.s        := io.signed
  div.io.dividend := io.op1
  div.io.divider  := io.op2
  io.error        := div.io.error

  io.result.hi   := Mux(io.md, result(63, 32), div.io.remainder)
  io.result.lo   := Mux(io.md, result(31, 0) , div.io.quotient )
}

class DIVBlackBox extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val s         = Input(Bool())
    val dividend  = Input(UInt(32.W))
    val divider   = Input(UInt(32.W))
    val quotient  = Output(UInt(32.W))
    val remainder = Output(UInt(32.W))
    val error     = Output(Bool())
  })
  addResource("/Divider.v")

  override def desiredName: String = "bit32_divider"
}
package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._


class HiLoBundle extends Bundle {
  val hi = Valid(UInt(dataWidth.W))
  val lo = Valid(UInt(dataWidth.W))
}

class MDU extends Module {
  val io = IO(new Bundle {
    val op1    = Input(UInt(dataWidth.W))
    val op2    = Input(UInt(dataWidth.W))
    val signed = Input(Bool())
    val md     = Input(Bool())  // io.md===1.U : Mul

    val result = Output(new HiLoBundle)
  })

  // Mul
  val op1 = RegNext(io.op1, 0.U(32.W))
  val op2 = RegNext(io.op2, 0.U(32.W))
  val result = Mux(io.signed, (op1.asSInt * op2.asSInt).asUInt, op1 * op2)

  // Div
  val div = Module(new DIVBlackBox())
  div.io.rst          := reset
  div.io.clk          := clock
  div.io.signed_div_i := io.signed
  div.io.opdata1_i    := io.op1
  div.io.opdata2_i    := io.op2
  div.io.start_i      := 1.B  // io.enable
  div.io.annul_i      := 0.B  // io.flush

  io.result.hi.bits   := Mux(io.md, result(63, 32), div.io.result_o(63, 32))
  io.result.lo.bits   := Mux(io.md, result(31, 0) , div.io.result_o(31, 0))
  io.result.hi.valid  := Mux(io.md, true.B, div.io.ready_o)
  io.result.lo.valid  := Mux(io.md, true.B, div.io.ready_o)
}

class DIVBlackBox extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val rst          = Input(Reset())
    val clk          = Input(Clock())
    val signed_div_i = Input(Bool())
    val opdata1_i    = Input(UInt(32.W))
    val opdata2_i    = Input(UInt(32.W))
    val start_i      = Input(Bool())
    val annul_i      = Input(Bool())
    val result_o     = Output(UInt(64.W))
    val ready_o      = Output(Bool())
  })
  addResource("/divider.v")

  override def desiredName: String = "divider"
}

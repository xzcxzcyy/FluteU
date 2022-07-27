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
    val op1 = Input(UInt(dataWidth.W))
    val op2 = Input(UInt(dataWidth.W))
    val signed = Input(Bool())

    val result = Output(new HiLoBundle)
  })

  // Mul
  val op1 = RegNext(io.op1, 0.U(32.W))
  val op2 = RegNext(io.op2, 0.U(32.W))
  val result = Mux(io.signed, (op1.asSInt * op2.asSInt).asUInt, op1 * op2)
  io.result.hi := result(63, 32)
  io.result.lo := result(31, 0)
}

class Div extends Module {
  val io = IO(new Bundle {
    val x      = Input(UInt(dataWidth.W))
    val y      = Input(UInt(dataWidth.W))
    val signed = Input(Bool())

    val result = Output(new HiLoBundle)
    val error  = Output(Bool())
  })
  val div = Module(new DIVBlackBox())
  
  div.io.s := io.signed
  div.io.dividend := io.x
  div.io.divider := io.y
  io.result.hi.bits   := div.io.remainder
  io.result.lo.bits   := div.io.quotient
  io.result.hi.valid  := 1.B
  io.result.lo.valid  := 1.B
  io.error            := div.io.error
}

class MulDiv extends Module {
  val io = IO(new Bundle{
    val enable = Input(Bool())
    val x      = Input(UInt(dataWidth.W))
    val y      = Input(UInt(dataWidth.W))
    val signed = Input(Bool())

    val result = Output(new HiLoBundle)
  })

  val divider = Module(new Div)
  divider.io.x := io.x
  divider.io.y := io.y

  val hi = RegInit(0.B)
  val lo = RegInit(0.B)
  io.result.hi.valid := 0.B
  io.result.lo.valid := 0.B
  when(divider.io.result.hi.valid) {
    io.result.hi.valid := 1.B
    hi := divider.io.result.hi.bits
  }
  when(divider.io.result.lo.valid) {
    io.result.lo.valid := 1.B
    lo := divider.io.result.lo.bits
  }
  io.result.hi.bits := hi
  io.result.lo.bits := lo
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
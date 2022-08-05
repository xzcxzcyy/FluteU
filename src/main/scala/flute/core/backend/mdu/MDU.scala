package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class MDU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(DecoupledIO(new MDUIn))
    val res = Output(Valid(new HILORead))

    val flush = Input(Bool())
  })

  // Mul
  val op1     = io.in.bits.op1
  val op2     = io.in.bits.op2
  val multRes = Wire(UInt(64.W))
  multRes := Mux(io.in.bits.signed, (op1.asSInt * op2.asSInt).asUInt, op1 * op2)
  val multResValid = io.in.valid && io.in.bits.mul

  // Div
  val div = Module(new DIVBlackBox())

  val idle :: busy :: Nil = Enum(2)
  val state               = RegInit(idle)
  val divReqBuf           = RegInit(0.U.asTypeOf(new MDUIn))
  switch(state) {
    is(idle) {
      when(!io.flush && io.in.valid && !io.in.bits.mul) { // is div
        state     := busy
        divReqBuf := io.in.bits
      }
    }
    is(busy) {
      when(io.flush || div.io.ready_o) {
        state     := idle
        divReqBuf := 0.U.asTypeOf(new MDUIn)
      }
    }
  }

  div.io.rst          := reset
  div.io.clk          := clock
  div.io.signed_div_i := divReqBuf.signed
  div.io.opdata1_i    := divReqBuf.op1
  div.io.opdata2_i    := divReqBuf.op2
  div.io.start_i      := (state === busy)
  div.io.annul_i      := io.flush

  val divRes      = div.io.result_o
  val divResValid = div.io.ready_o && (state === busy)

  val resValid = multResValid || divResValid
  val res = MuxCase(
    0.U(64.W),
    Seq(
      multResValid -> multRes,
      divResValid  -> divRes
    )
  )
  io.res.valid   := resValid
  io.res.bits.hi := res(63, 32)
  io.res.bits.lo := res(31, 0)

  io.in.ready := (state === idle)

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

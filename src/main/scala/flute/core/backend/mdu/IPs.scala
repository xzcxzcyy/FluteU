package flute.core.backend.mdu

import chisel3._

class div_gen_s extends BlackBox {
  val io = IO(new Bundle {
    val aclk    = Input(Clock())
    val aresetn = Input(Reset())

    // S_AXIS_DIVISOR
    val s_axis_divisor_tdata  = Input(UInt(32.W))
    val s_axis_divisor_tready = Output(Bool())
    val s_axis_divisor_tvalid = Input(Bool())

    // S_AXIS_DIVIDEND
    val s_axis_dividend_tdata  = Input(UInt(32.W))
    val s_axis_dividend_tready = Output(Bool())
    val s_axis_dividend_tvalid = Input(Bool())

    // M_AXIS_DOUT
    val m_axis_dout_tdata  = Output(UInt(64.W))
    val m_axis_dout_tvalid = Output(Bool())
  })
}

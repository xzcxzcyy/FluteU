package flute.core.backend.mdu

import chisel3._
import chisel3.util._

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

class div_gen_u extends BlackBox {
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

// 4 pipline
class mult_gen_s extends BlackBox {
  val io = IO(new Bundle {
    val CLK  = Input(Clock())
    val SCLR = Input(Bool())

    val A = Input(UInt(32.W))
    val B = Input(UInt(32.W))

    val P = Output(UInt(64.W))
  })
}

class mult_gen_u extends BlackBox {
  val io = IO(new Bundle {
    val CLK  = Input(Clock())
    val SCLR = Input(Bool())

    val A = Input(UInt(32.W))
    val B = Input(UInt(32.W))

    val P = Output(UInt(64.W))
  })
}

class MultIp extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(DecoupledIO(new MDUIn))
    val res = Output(Valid(new HILORead))

    val flush = Input(Bool())
  })
  io.in.ready := 1.B

  val mult  = Module(new mult_gen_s)
  val multu = Module(new mult_gen_u)

  mult.io.CLK   := clock
  mult.io.SCLR  := reset.asBool || io.flush
  multu.io.CLK  := clock
  multu.io.SCLR := reset.asBool || io.flush
  mult.io.A     := io.in.bits.op1
  mult.io.B     := io.in.bits.op2
  multu.io.A    := io.in.bits.op1
  multu.io.B    := io.in.bits.op2

  // -------------- Pipe --------------- //  multReq -> s1 -> s2 -> multReq
  val multReq = Wire(Valid(new MDUIn))
  multReq.bits  := io.in.bits
  multReq.valid := io.in.fire
  val s1       = RegInit(0.U.asTypeOf(Valid(new MDUIn)))
  val s2       = RegInit(0.U.asTypeOf(Valid(new MDUIn)))
  val s3       = RegInit(0.U.asTypeOf(Valid(new MDUIn)))
  val multResp = RegInit(0.U.asTypeOf(Valid(new MDUIn)))
  s1       := multReq
  s2       := s1
  s3       := s2
  multResp := s3

  when(io.flush) {
    s1       := 0.U.asTypeOf(Valid(new MDUIn))
    s2       := 0.U.asTypeOf(Valid(new MDUIn))
    multResp := 0.U.asTypeOf(Valid(new MDUIn))
  }
  // -------------- Pipe --------------- //

  val computeData = Mux(multResp.bits.signed, mult.io.P, multu.io.P)
  val respData    = Mux(multResp.valid, computeData, 0.U(64.W))

  io.res.valid   := multResp.valid
  io.res.bits.hi := respData(63, 32)
  io.res.bits.lo := respData(31, 0)
}

class DivIp extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(DecoupledIO(new MDUIn))
    val res = Output(Valid(new HILORead))

    val flush = Input(Bool())
  })

  val div  = Module(new div_gen_s)
  val divu = Module(new div_gen_u)

  div.io.aclk                   := clock
  div.io.aresetn                := !(reset.asBool || io.flush)
  div.io.s_axis_dividend_tvalid := io.in.valid && io.in.bits.signed
  div.io.s_axis_divisor_tvalid  := io.in.valid && io.in.bits.signed
  div.io.s_axis_dividend_tdata  := io.in.bits.op1
  div.io.s_axis_divisor_tdata   := io.in.bits.op2

  divu.io.aclk                   := clock
  divu.io.aresetn                := !(reset.asBool || io.flush)
  divu.io.s_axis_dividend_tvalid := io.in.valid && !io.in.bits.signed
  divu.io.s_axis_divisor_tvalid  := io.in.valid && !io.in.bits.signed
  divu.io.s_axis_dividend_tdata  := io.in.bits.op1
  divu.io.s_axis_divisor_tdata   := io.in.bits.op2

  io.in.ready := div.io.s_axis_dividend_tready && div.io.s_axis_divisor_tready &&
    divu.io.s_axis_dividend_tready && divu.io.s_axis_divisor_tready

  val resData = MuxCase(
    0.U(64.W),
    Seq(
      div.io.m_axis_dout_tvalid  -> div.io.m_axis_dout_tdata,
      divu.io.m_axis_dout_tvalid -> divu.io.m_axis_dout_tdata,
    )
  )

  io.res.valid := div.io.m_axis_dout_tvalid || divu.io.m_axis_dout_tvalid
  io.res.bits.hi := resData(31, 0) // 余数
  io.res.bits.lo := resData(63, 32)
}

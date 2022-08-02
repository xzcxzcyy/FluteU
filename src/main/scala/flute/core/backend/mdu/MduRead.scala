package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.core.components.RegFileReadIO
import flute.core.components.MuxStageReg
import flute.core.components.MuxStageRegMode
import flute.cp0.CP0Read

class MduRead extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new MicroOp(rename = true)))
    val out = Decoupled(new MicroOp(rename = true))

    val prf  = Flipped(new RegFileReadIO)
    val hilo = Flipped(new HILORead)
    val cp0  = Flipped(new CP0Read)

    val flush = Input(Bool())
  })

  val uop = WireInit(io.in.bits)

  io.prf.r1Addr := io.in.bits.rsAddr
  io.prf.r2Addr := io.in.bits.rtAddr

  val prfRes  = WireInit(VecInit(Seq(io.prf.r1Data, io.prf.r2Data)))
  val cp0Res  = WireInit(io.cp0.data)
  val hiloRes = WireInit(VecInit(Seq(io.hilo.hi, io.hilo.lo))) // 0 -> hi, 1 -> lo

  val (op1, op2)  = MduUtil.getOp(uop, prfRes, hiloRes, cp0Res)
  val uopWithData = WireInit(uop)
  uopWithData.op1.op := op1
  uopWithData.op2.op := op2

  val stage = Module(new MuxStageReg(Valid(new MicroOp(rename = true))))

  // datapath
  stage.io.in.bits  := uopWithData
  stage.io.in.valid := 1.B

  io.out.bits  := stage.io.out.bits
  io.out.valid := stage.io.out.valid

  // 双端decoupled信号生成
  io.in.ready := ((!stage.io.out.valid) || (io.out.fire))

  // stage控制信号
  when(io.flush || (!io.in.fire && io.out.fire)) {
    stage.io.mode := MuxStageRegMode.flush
  }.elsewhen(io.in.fire && (io.out.fire || !stage.io.out.valid)) {
    stage.io.mode := MuxStageRegMode.next
  }.otherwise {
    stage.io.mode := MuxStageRegMode.stall
  }
}

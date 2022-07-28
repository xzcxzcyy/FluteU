package flute.core.backend.lsu

import chisel3._
import chisel3.util._

import flute.core.backend.decode.MicroOp
import flute.core.backend.rename.BusyTableReadPort
import flute.core.components._
import flute.core.backend.alu.AluIssueUtil

// 不同于AluIssue, LsuIssue包含LsuIssueStage
class LsuIssue extends Module {
  private val numOfAluPipeline = 2
  val io = IO(new Bundle {
    // 接收窗口; ready指定是否发射
    val in = Flipped(Decoupled(new MicroOp(rename = true)))

    val bt = Vec(2, Flipped(new BusyTableReadPort))

    val out = Decoupled(new MicroOp(rename = true))

    val flush = Input(Bool())
  })

  // stage 1: Issue
  val uop      = WireInit(io.in.bits)
  val avalible = Wire(Bool())
  val bt       = Wire(Vec(2, Bool()))
  io.bt(0).addr := uop.rsAddr
  io.bt(1).addr := uop.rtAddr
  bt(0)         := io.bt(0).busy
  bt(1)         := io.bt(1).busy
  // 计算 avalible
  val op1Avalible = AluIssueUtil.op1Ready(uop, bt)
  val op2Avalible = AluIssueUtil.op2Ready(uop, bt)

  avalible := op1Avalible && op2Avalible

  val stage = Module(new MuxStageReg(Valid(new MicroOp(rename = true))))

  // datapath
  stage.io.in.bits  := uop
  stage.io.in.valid := 1.B

  io.out.bits  := stage.io.out.bits
  io.out.valid := stage.io.out.valid

  // 双端decoupled信号生成
  io.in.ready := ((!stage.io.out.valid) || (io.out.fire)) && avalible

  // stage控制信号
  when(io.flush || (!io.in.fire && io.out.fire)) {
    stage.io.mode := MuxStageRegMode.flush
  }.elsewhen(io.in.fire && (io.out.fire || !stage.io.out.valid)) {
    stage.io.mode := MuxStageRegMode.next
  }.otherwise {
    stage.io.mode := MuxStageRegMode.stall
  }
}

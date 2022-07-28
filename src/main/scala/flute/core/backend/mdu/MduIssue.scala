package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.core.backend.rename.BusyTableReadPort
import flute.core.backend.alu.AluIssueUtil
import flute.core.components.MuxStageReg
import flute.core.components.MuxStageRegMode


class MduIssue extends Module {
	val io = IO(new Bundle {
		val in = Flipped(Decoupled(new MicroOp(rename = true)))
		val mdPlAvailable = Flipped(new MDPlAvailableIO)
		val bt = Vec(2, Flipped(new BusyTableReadPort))
		val out = Decoupled(new MicroOp(rename = true))
	})

	// stage 1: Issue
	val uop = WireInit(io.in.bits)
	val available = Wire(Bool())
	val bt = Wire(Vec(2, Bool()))
	io.bt(0).addr := uop.rsAddr 
	io.bt(1).addr := uop.rtAddr
	bt(0)         := io.bt(0).busy 
	bt(1)         := io.bt(1).busy
	// 计算 available
	val op1Avalible = AluIssueUtil.op1Ready(uop, bt)
	val op2Avalible = AluIssueUtil.op2Ready(uop, bt)
	available := op1Avalible && op2Avalible && io.mdPlAvailable.available

	val stage = Module(new MuxStageReg(Valid(new MicroOp(rename = true))))

	// datapath
	stage.io.in.bits  := uop 
	stage.io.in.valid := true.B

	io.out.bits  := stage.io.out.bits 
	io.out.valid := stage.io.out.valid 

	// 双端decoupled信号生成
	io.in.ready := (!stage.io.out.valid || io.out.fire) && available 

	// stage 控制信号 
	when(io.in.fire && (io.out.fire || !stage.io.out.valid)) {
		stage.io.mode := MuxStageRegMode.next
	}.elsewhen(!io.in.fire && io.out.fire) {
		stage.io.mode := MuxStageRegMode.flush
	}.otherwise {
		stage.io.mode := MuxStageRegMode.stall
	}
}

package flute.core.issue

import chisel3._
import chisel3.util._
import flute.core.decode._

class Dispatch(nWays: Int = 2, nQueue: Int = 4) extends Module {
  require(nWays == 2 && nQueue == 4)

  val io = IO(new Bundle {
    val in = Vec(nWays, Flipped(ValidIO(new MicroOp(rename = true))))

    // ready 标志当前拍Queue是否有空间
    // 0, 1 -> ALU Queue
    // 2 -> LSU Queue
    // 3 -> MDU Queue
    val out = Vec(nQueue, DecoupledIO(new MicroOp(rename = true)))

    val stallReq = Output(Bool())
  })

  val alu0Valid = Wire(Bool())
  val alu1Valid = Wire(Bool())
  val lsuValid  = Wire(Bool())
  val mduValid  = Wire(Bool())

  // with valid
  val isALU = Wire(Vec(nWays, Bool()))
  val isLSU = Wire(Vec(nWays, Bool()))
  val isMDU = Wire(Vec(nWays, Bool()))

  for (i <- 0 until nWays) {
    isALU(i) := io.in(i).bits.instrType === InstrType.alu && io.in(i).valid
    isLSU(i) := io.in(i).bits.instrType === InstrType.loadStore && io.in(i).valid
    isMDU(i) := io.in(i).bits.instrType === InstrType.mulDiv && io.in(i).valid
  }

  // alu ops dispatch
  io.out(0).bits := io.in(0).bits
  io.out(1).bits := io.in(1).bits

  alu0Valid := isALU(0)
  alu1Valid := isALU(1)

  // FSM 状态机
  val idle :: dualLSU :: dualMDU :: Nil = Enum(3)
  val state                             = RegInit(idle)

  // lsu
  val selLSU = Mux(isLSU(0) && state =/= dualLSU, 0.U, 1.U)
  io.out(2).bits := Mux(isLSU(0) && state =/= dualLSU, io.in(0).bits, io.in(1).bits)
  lsuValid       := isLSU(selLSU)

  // mdu
  val selMDU = Mux(isMDU(0) && state =/= dualMDU, 0.U, 1.U)
  io.out(3).bits := Mux(isMDU(0) && state =/= dualMDU, io.in(0).bits, io.in(1).bits)
  mduValid       := isMDU(selMDU)

  io.out(0).valid := alu0Valid
  io.out(1).valid := alu1Valid
  io.out(2).valid := lsuValid
  io.out(3).valid := mduValid

  switch(state) {
    is(idle) {
      when(isMDU(0) && isMDU(1) && io.out(3).fire) {
        state := dualMDU
      }.elsewhen(isLSU(0) && isLSU(1) && io.out(2).fire) {
        state := dualLSU
      }
    }

    is(dualLSU) {
      state := Mux(io.out(2).fire, idle, dualLSU)
    }

    is(dualMDU) {
      state := Mux(io.out(3).fire, idle, dualMDU)
    }
  }

  // note: 对于 0->非alu, 1->alu 指令的情况，仍将alu指令发往1,而不是发给0
  val congested = (alu0Valid && !io.out(0).ready) ||
    (alu1Valid && !io.out(1).ready) ||
    (lsuValid && !io.out(2).ready) ||
    (mduValid && !io.out(3).ready)

  io.stallReq := congested ||
    (state === idle && isMDU(0) && isMDU(1)) || (state === idle && isLSU(0) && isLSU(1))

}

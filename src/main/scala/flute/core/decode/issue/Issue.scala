package flute.core.decode.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp
import flute.core.components._
import flute.config.CPUConfig._

class Issue extends Module {
  val io = IO(new Bundle {
    val fromId       = Vec(2, Flipped(DecoupledIO(new MicroOp)))
    val toEx         = Vec(2, DecoupledIO(new MicroOp))
    val regFileWrite = Vec(superscalar, new RegFileWriteIO())
    val debug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })

  val hazardCheck  = Module(new HazardCheck)
  val writingBoard = Module(new WritingBoard)
  val regFile      = Module(new RegFile(superscalar, superscalar))

  io.debug := regFile.io.debug

  val uOps = for (i <- 0 until 2) yield io.fromId(i).bits

  // hazardChcek IO connection
  io.fromId <> hazardCheck.io.in

  hazardCheck.io.query <> writingBoard.io.query

  for (i <- 0 until 2) {
    io.toEx(i).valid            := hazardCheck.io.out(i).valid
    hazardCheck.io.out(i).ready := io.toEx(i).ready
  }

  // writingBoard IO connection
  for (i <- 0 until 2) {
    // check out
    writingBoard.io.checkOut(i).bits  := io.regFileWrite(i).writeAddr
    writingBoard.io.checkOut(i).valid := io.regFileWrite(i).writeEnable

    // check in
    writingBoard.io.checkIn(i).bits := uOps(i).writeRegAddr

    // 当下一拍当前微指令必定发射时,录入check in
    val writeRegEn  = uOps(i).regWriteEn
    val issuePermit = hazardCheck.io.out(i).valid && io.toEx(i).ready
    writingBoard.io.checkIn(i).valid := writeRegEn && issuePermit
  }

  // regFile IO connection
  regFile.io.write <> io.regFileWrite

  class Operands extends Bundle {
    val op1 = UInt(dataWidth.W)
    val op2 = UInt(dataWidth.W)
  }
  val opRes = Wire(Vec(2, new Operands))

  for (i <- 0 until 2) {
    regFile.io.read(i).r1Addr := uOps(i).rsAddr
    regFile.io.read(i).r2Addr := uOps(i).rtAddr

    val rs = regFile.io.read(i).r1Data
    opRes(i).op1 := Mux(uOps(i).op1.valid, uOps(i).op1.op, rs)

    val rt = regFile.io.read(i).r2Data
    opRes(i).op2 := Mux(uOps(i).op2.valid, uOps(i).op2.op, rt)
  }

  // toEx
  for (i <- 0 until 2) {
    io.toEx(i).bits.aluOp        := uOps(i).aluOp
    io.toEx(i).bits.bjCond       := uOps(i).bjCond
    io.toEx(i).bits.immediate    := uOps(i).immediate
    io.toEx(i).bits.loadMode     := uOps(i).loadMode
    io.toEx(i).bits.pc           := uOps(i).pc
    io.toEx(i).bits.regWriteEn   := uOps(i).regWriteEn
    io.toEx(i).bits.rsAddr       := uOps(i).rsAddr
    io.toEx(i).bits.rtAddr       := uOps(i).rtAddr
    io.toEx(i).bits.storeMode    := uOps(i).storeMode
    io.toEx(i).bits.writeRegAddr := uOps(i).writeRegAddr

    // operand
    io.toEx(i).bits.op1.op    := opRes(i).op1
    io.toEx(i).bits.op1.valid := 1.B
    io.toEx(i).bits.op2.op    := opRes(i).op2
    io.toEx(i).bits.op2.valid := 1.B
  }

}

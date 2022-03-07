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
  })

  val hazardCheck  = Module(new HazardCheck)
  val writingBoard = Module(new WritingBoard)
  val regFile      = Module(new RegFile(superscalar, superscalar))

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
    // TODO
  }
  

}

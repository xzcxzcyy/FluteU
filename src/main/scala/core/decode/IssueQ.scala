package core.decode

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.log2Up
import chisel3.util._

import config.CpuConfig._
import core.components.{ALUOp, StoreMode}
import chisel3.util.MuxCase

// 指令解码后的结果(需要修改)
class MicroOp extends Bundle {
  class ControlBundle extends Bundle {
    val regWriteEn    = Bool()
    val memToReg      = Bool()
    val storeMode     = UInt(StoreMode.width.W)
    val aluOp         = UInt(ALUOp.width.W)
    val aluYFromImm   = Bool()
    val aluXFromShamt = Bool()
  }

  val control      = new ControlBundle()
  val rs           = UInt(dataWidth.W)
  val rt           = UInt(dataWidth.W)
  val writeRegAddr = UInt(regAddrWidth.W)
  val immediate    = UInt(dataWidth.W)
  val shamt        = UInt(shamtWidth.W)
}

class IssueQEntry extends Bundle {
  val entry = new MicroOp()
  val ready = Bool()
}

class IssueQ extends Module {
  val io = IO(new Bundle {
    val dataOut  = Output(Vec(16, new IssueQEntry))
    val entryNum = Output(UInt(log2Up(16).W))

    // enqueue
    val readAddr = Input(Vec(2, ValidIO(UInt(log2Up(16).W))))
    val readData = Input(Vec(2, ValidIO(new IssueQEntry)))

    // issue
    val issueAddr = Input(Vec(2, ValidIO(UInt(log2Up(16).W))))
    val issueData = Output(Vec(2, ValidIO(new IssueQEntry)))
  })

  val mem      = Mem(16, new IssueQEntry)
  val entryNum = RegInit(0.U(log2Up(16).W))

  object MoveState {
    val stay       = 0.U(3.W)
    val preFirst   = 1.U(3.W)
    val preSecond  = 2.U(3.W)
    val readFirst  = 3.U(3.W)
    val readSecond = 4.U(3.W)
  }

  val ctrl = Wire(Vec(16, UInt(2.W)))

  for (i <- 0 until 16 - 2) {
    val data = mem.read(i.U)
    val pre1 = if (i > 15) data else mem.read((i + 1).U)
    val pre2 = if (i > 14) data else mem.read((i + 2).U)

    mem.write(
      i.U,
      MuxLookup(
        key = ctrl(i),
        default = data,
        mapping = Seq(
          MoveState.stay       -> data,
          MoveState.preFirst   -> pre1,
          MoveState.preSecond  -> pre2,
          MoveState.readFirst  -> io.readData(0).bits,
          MoveState.readSecond -> io.readData(1).bits
        )
      )
    )
  }

  for (i <- 0 until 2) {
    io.issueData(i).bits  := mem.read(io.issueAddr(i).bits)
    io.issueData(i).valid := io.issueAddr(i).valid
  }

}

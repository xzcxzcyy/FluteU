package flute.core.decode

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._

// A compress Queue with 2 enq port and 2 deq port
//
class CompressQ[T <: Data](entryType: T) extends Module {
  val io = IO(new Bundle {
    val dataOut  = Output(Vec(16, entryType))
    val entryNum = Output(UInt(log2Up(16).W))
    /// enqueue
    val enqData = Flipped(Vec(2, DecoupledIO(entryType)))

    /// issue
    val issueAddr = Input(Vec(2, ValidIO(UInt(log2Up(16).W))))
    // provide that issueAddr(0) < issueAddr(1)
    val issueData = Output(Vec(2, entryType))

    //debug
    val ctrl = Output(Vec(16, UInt(3.W)))
  })


  val mem      = Mem(16, entryType)
  val entryNum = RegInit(0.U(log2Up(16).W))

  object MoveState {
    val width = 3.W
    val stay       = 0.U(width)
    val preFirst   = 1.U(width)
    val preSecond  = 2.U(width)
    val readFirst  = 3.U(width)
    val readSecond = 4.U(width)
  }

  val ctrl = Wire(Vec(16, UInt(3.W)))
  // debug
  io.ctrl := ctrl

  for (i <- 0 until 16) {
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
          MoveState.readFirst  -> io.enqData(0).bits,
          MoveState.readSecond -> io.enqData(1).bits
        )
      )
    )
  }

  for (i <- 0 until 2) {
    io.issueData(i) := mem.read(io.issueAddr(i).bits)
  }

  val numDeq    = PopCount(io.issueAddr.map(_.valid)) // 下一拍出队数量，由仲裁单元保证合法
  val numAfterDeq = entryNum - numDeq

  // 内部ValidIO
  val enqAddr = Wire(Vec(2, ValidIO(UInt(log2Up(16).W))))
  for (i <- 0 to 1) {
    val index = numAfterDeq + i.U
    enqAddr(i).bits  := index

    val inBound = Mux(index < 16.U, 1.B, 0.B)
    enqAddr(i).valid := inBound && io.enqData(i).valid

    // 同时给出enqData的ready信号
    // 该ready信号与enqData.valid信号独立,不会产生组合环
    io.enqData(i).ready := inBound
  }

  val numEnq = PopCount(io.enqData.map(_.valid))

  // 更新状态
  entryNum := entryNum + numEnq - numDeq
  io.entryNum := entryNum

  val issueAddr = Wire(Vec(2, UInt(5.W)))
  for (i <- 0 until 2) issueAddr(i) := Mux(io.issueAddr(i).valid, io.issueAddr(i).bits, 16.U)

  for (i <- 0 until 16) {
    ctrl(i) := MuxCase(
      default = MoveState.stay,
      mapping = Seq(
        (enqAddr(0).valid && i.U === enqAddr(0).bits)     -> MoveState.readFirst,
        (enqAddr(1).valid && i.U === enqAddr(1).bits)     -> MoveState.readSecond,
        (i.U < issueAddr(0))                              -> MoveState.stay,
        (i.U >= issueAddr(0) && i.U < issueAddr(1) - 1.U) -> MoveState.preFirst,
        (i.U >= issueAddr(1) - 1.U)                       -> MoveState.preSecond
      )
    )
    // io.ctrl(i) := ctrl(i)
  }

  for (i <- 0 until 16) {
    io.dataOut(i) := mem.read(i.U)
  }

}

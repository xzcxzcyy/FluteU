package flute.core.backend.alu

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.backend.decode.MicroOp
import flute.core.backend.rename.BusyTableReadPort

class AluIssueQueue(volume: Int, detectWidth: Int) extends Module {
  private val enqNum = 2

  private val deqNum = 2

  private val entryType = new MicroOp(rename = true)

  private val width = entryType.getWidth

  val io = IO(new Bundle {
    val enq   = Vec(enqNum, Flipped(DecoupledIO(entryType)))
    val data  = Vec(detectWidth, Output(Valid(entryType)))
    val issue = Vec(deqNum, Input(Valid(UInt(log2Ceil(detectWidth).W))))
    val flush = Input(Bool())
  })
  val queue = Module(new AluCompressIssueQueue(UInt(width.W), volume, detectWidth))

  queue.io.flush := io.flush
  queue.io.issue := io.issue

  for (i <- 0 until enqNum) {
    queue.io.enq(i).valid := io.enq(i).valid
    io.enq(i).ready       := queue.io.enq(i).ready
    queue.io.enq(i).bits  := io.enq(i).bits.asUInt
  }

  for (i <- 0 until detectWidth) {
    io.data(i).valid := queue.io.data(i).valid
    io.data(i).bits  := queue.io.data(i).bits.asTypeOf(entryType)
  }
}

class AluCompressIssueQueue[T <: Data](entryType: T, volume: Int, detectWidth: Int) extends Module {
  private val enqNum = 2

  private val deqNum = 2

  val io = IO(new Bundle {
    val enq   = Vec(enqNum, Flipped(DecoupledIO(entryType)))
    val data  = Vec(detectWidth, Output(Valid(entryType)))
    val issue = Vec(deqNum, Input(Valid(UInt(log2Ceil(detectWidth).W))))

    val flush = Input(Bool())
  })

  val ram      = Reg(Vec(volume, entryType))
  val entryNum = RegInit(0.U(log2Ceil(volume).W))

  val ramNext = WireInit(ram)

  val issued = Wire(Vec(deqNum, UInt(log2Ceil(volume + 1).W)))
  for (i <- 0 until deqNum) issued(i) := Mux(io.issue(i).valid, io.issue(i).bits, volume.U)

  val numDeqed    = PopCount(io.issue.map(_.valid))
  val numAfterDeq = entryNum - numDeqed

  // valid & ready
  for (i <- 0 until enqNum) {
    io.enq(i).ready := (numAfterDeq + i.U) < volume.U
  }
  for (i <- 0 until detectWidth) {
    io.data(i).valid := i.U < entryNum
    io.data(i).bits  := ram(i)
  }
  val numEnqed = PopCount(io.enq.map(_.fire))

  val offset = Wire(Vec(volume, UInt(log2Ceil(volume).W)))

  offset := AluIssueQueueComponents.entryMove(issued, deqNum.U, volume)

  for (i <- 0 until volume) {
    val nextIdx = i.U + offset(i)
    when(i.U === numAfterDeq) {
      ramNext(i) := io.enq(0).bits
    }.elsewhen(i.U === numAfterDeq + 1.U) {
      ramNext(i) := io.enq(1).bits
    }.otherwise {
      ramNext(i) := ram(nextIdx)
    }
  }
  entryNum := Mux(io.flush, 0.U, entryNum - numDeqed + numEnqed)
  ram      := ramNext
}

object AluIssueQueueComponents {
  def entryMove(issued: Seq[UInt], issueCount: UInt, volume: Int) = {
    assert(issued.length == 2)
    val n        = issued.length
    val movement = Seq.fill(volume)(WireInit(0.U(log2Up(volume).W)))
    when(issueCount === 1.U) {
      for (i <- 0 until volume) yield {
        movement(i) := Mux(i.U >= issued(0), 1.U, 0.U)
      }
    }.elsewhen(issueCount === 2.U) {
      for (i <- 0 until volume) yield {
        when((i + 1).U >= issued(1)) {
          movement(i) := 2.U
        }.elsewhen(i.U >= issued(0)) {
          movement(i) := 1.U
        }.otherwise {
          movement(i) := 0.U
        }
      }
    }
    movement
  }

}

// 1 2 3 4 5 6 7
//     3   5 - out
// 1 2 4 6 7
//
// 1 2 3 4 5 6 7 8
//     3     6 - out
// 1 2 4 5 7 8
// 1 2 3 4 5 6 7 8 9 a b c
//   2     5     8 - out
// 1 3 4 6 7 9 a b c
// 0 1 1 2 2 3 3 3 3

// 1 2 3 4 5 6 7 8 9 a b c
//   2     5 6 - out
// 1 3 4 7 8 9 a b c
// 0 1 1 3 3 3 3 3 3
// 0 0 0 0 0 0 0 0 0
// 0 1 1 1 1 1 1 1 1
// 0 0 0 1 1 1 1 1 1
// 0 0 0 1 1 1 1 1 1

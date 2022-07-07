package flute.core.issue

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.decode.MicroOp
import flute.core.rename.BusyTableReadPort

class AluIqEntry extends Bundle {
  val uop    = new MicroOp
  val awaken = Bool()
}

class AluIssueQueue(volume: Int, detectWidth: Int) extends Module {
  private val enqNum = 2

  private val deqNum = 2

  val io = IO(new Bundle {
    val enq = Vec(enqNum, Flipped(DecoupledIO(new MicroOp)))
    val deq = Vec(deqNum, DecoupledIO(new MicroOp))
  })

  val ram = RegInit(VecInit(Seq.fill(volume)(0.U.asTypeOf(new AluIqEntry))))

  val ramNext = WireInit(ram)

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

  def canIssue(entry: AluIqEntry, bt: Seq[Bool]) = {
    assert(bt.length == 2)

    val r1PrfValid = entry.uop.op1.valid || bt(0)
    val r2PrfValid = entry.uop.op2.valid || bt(1)

    entry.awaken || (r1PrfValid && r2PrfValid)
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

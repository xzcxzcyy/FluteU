package flute.core.issue

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.decode.MicroOp

class AluIqEntry extends Bundle {
  val uop = new MicroOp
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


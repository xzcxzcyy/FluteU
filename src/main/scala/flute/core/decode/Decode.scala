package flute.core.decode

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.components.{RegFile, RegFileWriteIO}
import flute.core.fetch.FetchIO

// the io that with next
class DecodeIO extends Bundle {
  val microOps = Vec(superscalar, DecoupledIO(new MicroOp))
}

class DecodeFeedbackIO extends Bundle {}

class Decode extends Module {
  val io = IO(new Bundle {
    val withExecute  = new DecodeIO()
    val withFetch    = Flipped(new FetchIO())
    val feedback     = new DecodeFeedbackIO()
    val regFileWrite = Vec(superscalar, new RegFileWriteIO())

    val debug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })

  val regFile = Module(new RegFile(superscalar, superscalar))

  val issueQueue = Module(new BubbleIssueQueue)

  val decoders = for (i <- 0 until superscalar) yield Module(new Decoder)

  val willProcess = Mux(io.withFetch.instNum < 2.U, io.withFetch.instNum, 2.U)
  // willProcess = min(2, instNum)

  io.debug := regFile.io.debug

  for (i <- 0 until superscalar) {
    // read
    regFile.io.read(i) <> issueQueue.io.regFileRead(i)
    // write
    regFile.io.write(i) := io.regFileWrite(i)
    issueQueue.io.regFileWrite(i) := io.regFileWrite(i) // 更新其writingBoard

    // fetch
    decoders(i).io.instr := io.withFetch.insts(i)

    // issue
    issueQueue.io.in(i).bits  := decoders(i).io.microOp
    issueQueue.io.in(i).valid := Mux(willProcess > i.U, 1.B, 0.B)

    // ex
    io.withExecute.microOps(i) <> issueQueue.io.out(i)
  }

  io.withFetch.willProcess := willProcess

}

package flute.util

import chisel3._
import chisel3.util._

class FIFOBundle[T <: Data](gen: T, numRead: Int, numWrite:Int) extends Bundle {
  val read  = Vec(numRead, Decoupled(gen))
  val write = Flipped(Vec(numWrite, Decoupled(gen)))
}

class FIFOQueue[T <: Data](gen:T, numEntries: Int, numRead: Int, numWrite: Int) extends Module {
  val io = IO(new FIFOBundle(gen, numRead, numWrite))

  val data = Mem(numEntries, gen)

  val head_ptr = RegInit(0.U(log2Up(numEntries).W))
  val tail_ptr = RegInit(0.U(log2Up(numEntries).W))

  val difference   = head_ptr - tail_ptr
  val deqEntries = Mux(tail_ptr < head_ptr, difference, numEntries.U - difference)
  val enqEntries = Mux(tail_ptr < head_ptr, difference, numEntries.U - difference)

  val numTryEnq = PopCount(io.write.map(_.valid))
  val numTryDeq = PopCount(io.read.map(_.ready))
  val numDeq = Mux(deqEntries < numTryDeq, difference, numEntries.U - difference)
  val numEnq = Mux(enqEntries < numTryEnq, difference, numEntries.U - difference)

  for (i <- 0 until numWrite) {
    val offset = Wire(UInt(log2Up(numEntries).W))
    if (i == 0) {
      offset := 0.U
    } else {
      offset := PopCount(io.write.map(_.valid)(i-1))
    }

    when (io.write(i).valid) {
      when (offset < numEnq) {
        data(head_ptr + offset) := io.write(i).bits
        io.write(i).ready := 1.B
      }.otherwise{
        io.write(i).ready := 0.B
      }
    }.otherwise{
        io.write(i).ready := 0.B
    }
  }

  for (i <- 0 until numRead) {
    val offset = Wire(UInt(log2Up(numEntries).W))
    if (i == 0) {
      offset := 0.U
    } else {
      offset := PopCount(io.read.map(_.ready)(i-1))
    }

    when (io.read(i).ready) {
      when (offset < numDeq) {
        io.read(i).bits := data(tail_ptr + offset)
        io.read(i).valid := 1.B
      }.otherwise{
        io.read(i).bits := DontCare
        io.read(i).valid := 0.B
      }
    }.otherwise{
      io.read(i).bits := DontCare
      io.read(i).valid := 0.B
    }
  }

  head_ptr := head_ptr + numEnq
  tail_ptr := tail_ptr + numDeq
}
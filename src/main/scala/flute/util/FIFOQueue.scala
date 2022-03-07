package flute.util

import chisel3._
import chisel3.util._

class FIFOBundle[T <: Data](gen: T, numRead: Int, numWrite:Int) extends Bundle {
  val read  = Vec(numRead, Decoupled(gen))
  val write = Flipped(Vec(numWrite, Decoupled(gen)))
}

class FIFOQueue[T <: Data](gen:T, numEntries: Int, numRead: Int, numWrite: Int) extends Module {
  assert(isPow2(numRead) && numRead > 1)
  assert(isPow2(numWrite) && numWrite > 1)
  assert(isPow2(numEntries) && numEntries > numRead + numWrite)

  val io = IO(new FIFOBundle(gen, numRead, numWrite))

  val data = Mem(numEntries, gen)

  val writeReady = RegInit(VecInit(Seq.fill(numWrite)(true.B)))
  val readValid  = RegInit(VecInit(Seq.fill(numRead)(false.B)))

  for (i <- 0 until numWrite) {
    io.write(i).ready := writeReady(i)
  }

  for (i <- 0 until numRead) {
    io.read(i).valid := readValid(i)
  }

  // tail是数据入队的位置（该位置目前没数据），head是数据出队的第一个位置（该位置放了最老的数据）
  val head_ptr = RegInit(0.U(log2Up(numEntries).W))
  val tail_ptr = RegInit(0.U(log2Up(numEntries).W))
  val read_ptr = RegInit(0.U(log2Up(numEntries).W))

  val numValid = Mux(tail_ptr < head_ptr, numEntries.U - head_ptr + tail_ptr, tail_ptr - head_ptr)

  val numTryEnq = PopCount(io.write.map(_.valid))
  val maxEnq = numEntries.U - numValid - 1.U
  val numEnq = Mux(maxEnq < numTryEnq, maxEnq, numTryEnq)

  val numTryDeq = PopCount(io.read.map(_.ready))
  val maxDeq = numValid + numEnq
  val numDeq = Mux(maxDeq < numTryDeq, maxDeq, numTryDeq)

  for (i <- 0 until numWrite) {
    val offset = Wire(UInt(log2Up(numEntries).W))
    if (i == 0) {
      offset := 0.U
    } else {
      offset := PopCount(io.write.slice(0, i).map(_.valid))
    }

    when (io.write(i).valid) {
      when (offset < numEnq) {
        data(tail_ptr + offset) := io.write(i).bits
        writeReady(i) := true.B
      }.otherwise {
        writeReady(i) := false.B
      }
    }.otherwise {
      writeReady(i) := true.B
    }
  }

  for (i <- 0 until numRead) {
    val offset = Wire(UInt(log2Up(numEntries).W))
    if (i == 0) {
      offset := 0.U
    } else {
      offset := PopCount(io.write.slice(0, i).map(_.ready))
    }

    when (io.read(i).ready) {
      when (offset < numDeq) {
        io.read(i).bits := data(read_ptr + offset)
        readValid(i) := true.B
      }.otherwise{
        io.read(i).bits := DontCare
        readValid(i) := false.B
      }
    }.otherwise{
      io.read(i).bits := DontCare
      readValid(i) := false.B
    }
  }

  when(numDeq > 0.U) {
    read_ptr := head_ptr
  }

  head_ptr := head_ptr + numDeq
  tail_ptr := tail_ptr + numEnq
}
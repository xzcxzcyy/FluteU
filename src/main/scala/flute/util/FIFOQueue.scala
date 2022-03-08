package flute.util

import chisel3._
import chisel3.util._

class FIFOBundle[T <: Data](gen: T, numRead: Int, numWrite:Int) extends Bundle {
  val read  = Vec(numRead, Decoupled(gen))
  val write = Flipped(Vec(numWrite, Decoupled(gen)))
}

class FIFOWriteBufferIO[T <: Data](gen: T, numWrite:Int) extends Bundle {
  val internal = Vec(numWrite, Decoupled(gen))
  val external = Flipped(Vec(numWrite, Decoupled(gen)))
}

class FIFOWriteBuffer[T <: Data](gen:T, numWrite:Int) extends Module {
  val io = IO(new FIFOWriteBufferIO(gen, numWrite))

  val writeBits  = Mem(numWrite, gen)
  val writeValid = RegInit(VecInit(Seq.fill(numWrite)(false.B)))

  for (i <- 0 until numWrite) {
    when(writeValid(i) === false.B || (writeValid(i) && io.internal(i).ready)) {
      io.internal(i).valid := io.external(i).valid
      io.internal(i).bits  := io.external(i).bits
      writeValid(i) := io.external(i).valid
      writeBits(i)  := io.external(i).bits
    }.otherwise{
      io.internal(i).valid := writeValid(i)
      io.internal(i).bits  := writeBits(i)
    }

    io.external(i).ready := io.internal(i).ready
  }
}

class FIFOQueue[T <: Data](gen:T, numEntries: Int, numRead: Int, numWrite: Int, withWriteBuffer:Boolean = false) extends Module {
  assert(isPow2(numRead) && numRead > 1)
  assert(isPow2(numWrite) && numWrite > 1)
  assert(isPow2(numEntries) && numEntries > numRead + numWrite)

  val io = IO(new FIFOBundle(gen, numRead, numWrite))

  val writePorts = if(withWriteBuffer){
    val writeBuffer = Module(new FIFOWriteBuffer(gen, numWrite))
    for (i <- 0 until numWrite) {
      writeBuffer.io.external(i).valid := io.write(i).valid
      writeBuffer.io.external(i).bits  := io.write(i).bits
      io.write(i).ready                := writeBuffer.io.external(i).ready
    }
    writeBuffer.io.internal
  } else io.write

  val data = Mem(numEntries, gen)

  val writeReady = RegInit(VecInit(Seq.fill(numWrite)(true.B)))
  val readValid  = RegInit(VecInit(Seq.fill(numRead)(false.B)))
  val readAddrs  = RegInit(VecInit(Seq.fill(numRead)(0.U(log2Up(numEntries).W))))

  for (i <- 0 until numWrite) {
    writePorts(i).ready := writeReady(i)
  }

  for (i <- 0 until numRead) {
    io.read(i).valid := readValid(i)
  }

  // tail是数据入队的位置（该位置目前没数据），head是数据出队的第一个位置（该位置放了最老的数据）
  val head_ptr = RegInit(0.U(log2Up(numEntries).W))
  val tail_ptr = RegInit(0.U(log2Up(numEntries).W))

  val numValid = Mux(tail_ptr < head_ptr, numEntries.U - head_ptr + tail_ptr, tail_ptr - head_ptr)

  val numTryEnq = PopCount(writePorts.map(_.valid))
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
      offset := PopCount(writePorts.slice(0, i).map(_.valid))
    }

    when (io.write(i).valid) {
      when (offset < numEnq) {
        data(tail_ptr + offset) := writePorts(i).bits
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
      offset := PopCount(writePorts.slice(0, i).map(_.ready))
    }

    when (io.read(i).ready) {
      when (offset < numDeq) {
        readAddrs(i) := head_ptr + offset
        io.read(i).bits := data(readAddrs(i))
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

  head_ptr := head_ptr + numDeq
  tail_ptr := tail_ptr + numEnq
}
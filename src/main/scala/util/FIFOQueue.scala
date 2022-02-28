package flute.util

import chisel3._
import chisel3.util._

class FIFOBundle[T <: Data](gen: T, numRead: Int, numWrite:Int) extends Bundle {
  val read  = Vec(numRead, Decoupled(gen))
  val write = Flipped(Vec(numWrite, Decoupled(gen)))
  // val test  = Output(UInt(32.W))
}

class FIFOQueue[T <: Data](gen:T, numEntries: Int, numRead: Int, numWrite: Int) extends Module {
  assert(isPow2(numEntries) && numEntries > 1)

  val io = IO(new FIFOBundle(gen, numRead, numWrite))

  // io.test := 2.U(3.W) - 5.U(3.W)

  val data = Mem(numEntries, gen)

  // tail是数据入队的位置（该位置目前没数据），head是数据出队的第一个位置（该位置放了最老的数据）
  val head_ptr = RegInit(0.U(log2Up(numEntries).W))
  val tail_ptr = RegInit(0.U(log2Up(numEntries).W))

  // 为了区分空队列和满队列满，队列不允许真正全部放满（numEntries=8时，有8个位置，但同一时刻最多只能用7个）
  val difference = tail_ptr - head_ptr
  val deqEntries = difference
  val enqEntries = (numEntries - 1).U - difference

  val numTryEnq = PopCount(io.write.map(_.valid))
  val numTryDeq = PopCount(io.read.map(_.ready))
  val numDeq = Mux(deqEntries < numTryDeq, deqEntries, numTryDeq)
  val numEnq = Mux(enqEntries < numTryEnq, enqEntries, numTryEnq)

  // Assumptions:
  // 读写端口的均需要把1集中放前面
  // 类似[1, 0, 1]的读写行为是未定义的
  for (i <- 0 until numWrite) {
    val offset = i.U

    when (io.write(i).valid) {
      when (offset < numEnq) {
        data((tail_ptr + offset)(log2Up(numEntries) - 1, 0)) := io.write(i).bits
      }
    }

    io.write(i).ready := offset < numEnq
  }

  for (i <- 0 until numRead) {
    val offset = i.U

    when (io.read(i).ready) {
      when (offset < numDeq) {
        io.read(i).bits := data((head_ptr + offset)(log2Up(numEntries) - 1, 0))
      }.otherwise{
        io.read(i).bits := DontCare
      }
    }.otherwise{
      io.read(i).bits := DontCare
    }

    io.read(i).valid := offset < numDeq
  }

  head_ptr := head_ptr + numDeq
  tail_ptr := tail_ptr + numEnq
}
package flute.core.rob

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._


class InstrBank extends Bundle {
  val complete  = Reg(Bool())
  val logicReg  = Reg(UInt(LogicRegIdxWidth.W))
  val physicReg = Reg(UInt(PhyRegIdxWidth.W))
  val originReg = Reg(UInt(PhyRegIdxWidth.W))
  val exception = Reg(UInt(exceptionIdxWidth.W))
  val instrType = Reg(UInt(instrTypeWidth.W))
}
class InstrBankData extends Bundle {
  val complete  = Bool()
  val logicReg  = UInt(LogicRegIdxWidth.W)
  val physicReg = UInt(PhyRegIdxWidth.W)
  val originReg = UInt(PhyRegIdxWidth.W)
  val exception = UInt(exceptionIdxWidth.W)
  val instrType = UInt(instrTypeWidth.W)
}

class ROBEntry extends Bundle {
  val pc         = Reg(UInt(addrWidth.W))  // 一行指令中第一个的PC
  val instrBanks = Vec(superscalar, new InstrBank)
}
class ROBEntryData extends Bundle {
  val pc         = UInt(addrWidth.W)  // 一行指令中第一个的PC
  val instrBanks = Vec(superscalar, new InstrBankData)
}

class ROB[T <: Data](numEntries: Int, numRead: Int, numWrite: Int) extends Module {
  assert(isPow2(numEntries) && numEntries > 1)

  val io = IO(new Bundle{
    val read  = Vec(numRead, Decoupled(new ROBEntryData))
    val write = Flipped(Vec(numWrite, Decoupled(new ROBEntryData)))
    val stall = Output(Bool())
  })

  val entries = Vec(numEntries, new ROBEntry)

  // tail是数据入队的位置（该位置目前没数据），head是数据出队的第一个位置（该位置放了最老的数据）
  val head_ptr = RegInit(0.U(log2Up(numEntries).W))
  val tail_ptr = RegInit(0.U(log2Up(numEntries).W))

  // 为了区分空队列和满队列满，队列不允许真正全部放满（numEntries=8时，有8个位置，但同一时刻最多只能用7个）
  val difference = tail_ptr - head_ptr
  val deqEntries = difference
  val enqEntries = (numEntries - 1).U - difference

  val numTryEnq = PopCount(io.write.map(_.valid))
  val numTryDeq = PopCount(io.read.map(_.ready))
  val numDeq    = Mux(deqEntries < numTryDeq, deqEntries, numTryDeq)
  val numEnq    = Mux(enqEntries < numTryEnq, enqEntries, numTryEnq)

  // Assumptions:
  // 读写端口的均需要把1集中放前面
  // 类似[1, 0, 1]的读写行为是未定义的
  for (i <- 0 until numWrite) {
    val offset = i.U
    when(io.write(i).valid) {
      when(offset < numEnq) {
        entries((tail_ptr + offset)(log2Up(numEntries) - 1, 0)) := io.write(i).bits
      }
    }
    io.write(i).ready := offset < enqEntries
  }

  for (i <- 0 until numRead) {
    val offset = i.U
    when(offset < numDeq) {
      io.read(i).bits := entries((head_ptr + offset)(log2Up(numEntries) - 1, 0))
    }.otherwise {
      io.read(i).bits := DontCare
    }
    io.read(i).valid := offset < deqEntries
  }

  io.stall := io.read.map(_.valid).reduce((x,y) => x||y)

  head_ptr := head_ptr + numDeq
  tail_ptr := tail_ptr + numEnq

  // 判断那些指令可以退休
  io.read(0).valid := entries(head_ptr).instrBanks(0).complete
  for(i <- 1 to superscalar) {
    io.read(i).valid := entries(head_ptr).instrBanks(i).complete & entries(head_ptr).instrBanks(i-1).complete
  }
}
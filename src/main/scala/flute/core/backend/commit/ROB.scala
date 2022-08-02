package flute.core.backend.commit

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.cp0.ExceptionBundle
import flute.util.ValidBundle
import flute.core.backend.decode.StoreMode
import flute.core.backend.decode.MDUOp

class ROBEntry extends Bundle {
  val pc        = UInt(addrWidth.W)
  val complete  = Bool()
  val logicReg  = UInt(archRegAddrWidth.W)
  val physicReg = UInt(phyRegAddrWidth.W)
  val originReg = UInt(phyRegAddrWidth.W)
  val exception = new ExceptionBundle
  val instrType = UInt(instrTypeWidth.W)
  val regWEn    = Bool()
  val memWMode = UInt(StoreMode.width.W)
  val memWAddr = UInt(addrWidth.W)
  val memWData = UInt(dataWidth.W)

  // branch
  val branch      = Bool()
  val predictBT   = UInt(addrWidth.W)
  val computeBT   = UInt(addrWidth.W)
  val branchTaken = Bool()
  val inSlot      = Bool()

  // hi, lo, cp0; valid for enable
  val hiRegWrite  = Valid(UInt(32.W))
  val loRegWrite  = Valid(UInt(32.W))
  val cp0RegWrite = Valid(UInt(32.W))
  val cp0Sel      = UInt(3.W)
  val cp0Addr     = UInt(5.W)

  // result debug
  val regWData = UInt(dataWidth.W)
}

class ROBWrite(numEntries: Int) extends Bundle {
  val bits    = Input(new ROBEntry)
  val valid   = Input(Bool())
  val ready   = Output(Bool())
  val robAddr = Output(UInt(log2Up(numEntries).W))
}
/// once complete bundle valid, complete shall be set automatically
class ROBCompleteBundle(robAddrWidth: Int = robEntryNumWidth) extends Bundle {
  val valid     = Bool()
  val robAddr   = UInt(robAddrWidth.W)
  val exception = new ExceptionBundle
  val memWAddr = UInt(addrWidth.W)
  val memWData = UInt(dataWidth.W)

  // branch
  val computeBT   = UInt(addrWidth.W)
  val branchTaken = Bool()

  // hi, lo, cp0
  val hiRegWrite  = Valid(UInt(32.W))
  val loRegWrite  = Valid(UInt(32.W))
  val cp0RegWrite = Valid(UInt(32.W))

  // result debug
  val regWData = UInt(dataWidth.W)
}

class ROB(numEntries: Int, numRead: Int, numWrite: Int, numSetComplete: Int) extends Module {
  assert(isPow2(numEntries) && numEntries > 1)

  val io = IO(new Bundle {
    val read        = Vec(numRead, Decoupled(new ROBEntry))
    val write       = Vec(numWrite, new ROBWrite(numEntries))
    val setComplete = Vec(numSetComplete, Input(new ROBCompleteBundle(log2Up(numEntries))))
    val flush       = Input(Bool())
  })

  val entries = Mem(numEntries, new ROBEntry)

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
    io.write(i).ready   := offset < enqEntries
    io.write(i).robAddr := (tail_ptr + offset)(log2Up(numEntries) - 1, 0)
  }

  for (i <- 0 until numRead) {
    val offset = i.U

    io.read(i).bits  := entries((head_ptr + offset)(log2Up(numEntries) - 1, 0))
    io.read(i).valid := offset < deqEntries
  }

  head_ptr := Mux(io.flush, 0.U, head_ptr + numDeq)
  tail_ptr := Mux(io.flush, 0.U, tail_ptr + numEnq)

  for (port <- io.setComplete) {
    when(port.valid) {
      entries(port.robAddr).complete  := 1.B
      entries(port.robAddr).exception := port.exception
      entries(port.robAddr).memWAddr    := port.memWAddr
      entries(port.robAddr).memWData    := port.memWData
      entries(port.robAddr).branchTaken := port.branchTaken
      entries(port.robAddr).computeBT   := port.computeBT
      entries(port.robAddr).regWData    := port.regWData
      entries(port.robAddr).hiRegWrite  := port.hiRegWrite
      entries(port.robAddr).loRegWrite  := port.loRegWrite
      entries(port.robAddr).cp0RegWrite := port.cp0RegWrite
    }
  }
}

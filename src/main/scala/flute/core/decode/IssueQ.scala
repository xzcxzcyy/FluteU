package flute.core.decode

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.log2Up
import chisel3.util._

import flute.config.CPUConfig._
import flute.core.components.ALUOp
import chisel3.util.MuxCase
import flute.core.components._
import flute.core.decode.StoreMode

class IssueQEntry extends Bundle {
  val entry = new MicroOp()
  val ready = Bool()
}

class IssueQ[T <: Data](entryType: T) extends Module {
  val io = IO(new Bundle {
    val dataOut = Output(Vec(16, entryType))
    // val ctrl = Output(Vec(16, UInt(3.W)))
    val entryNum = Output(UInt(log2Up(16).W))

    /// enqueue
    val enqAddr = Input(Vec(2, ValidIO(UInt(log2Up(16).W))))
    val enqData = Input(Vec(2, ValidIO(entryType)))

    /// issue
    val issueAddr = Input(Vec(2, ValidIO(UInt(log2Up(16).W))))
    // provide that issueAddr(0) < issueAddr(1)
    val issueData = Output(Vec(2, ValidIO(entryType)))
  })

  val mem      = Mem(16, entryType)
  val entryNum = RegInit(0.U(log2Up(16).W))

  object MoveState {
    val stay       = 0.U(3.W)
    val preFirst   = 1.U(3.W)
    val preSecond  = 2.U(3.W)
    val readFirst  = 3.U(3.W)
    val readSecond = 4.U(3.W)
  }

  val ctrl = Wire(Vec(16, UInt(3.W)))

  for (i <- 0 until 16) {
    val data = mem.read(i.U)
    val pre1 = if (i > 15) data else mem.read((i + 1).U)
    val pre2 = if (i > 14) data else mem.read((i + 2).U)

    mem.write(
      i.U,
      MuxLookup(
        key = ctrl(i),
        default = data,
        mapping = Seq(
          MoveState.stay       -> data,
          MoveState.preFirst   -> pre1,
          MoveState.preSecond  -> pre2,
          MoveState.readFirst  -> io.enqData(0).bits,
          MoveState.readSecond -> io.enqData(1).bits
        )
      )
    )
  }

  for (i <- 0 until 2) {
    io.issueData(i).bits  := mem.read(io.issueAddr(i).bits)
    io.issueData(i).valid := io.issueAddr(i).valid
  }

  val numEnq = PopCount(io.enqAddr.map(_.valid))   // 下一拍入队数量，由分配单元保证合法
  val numDeq = PopCount(io.issueAddr.map(_.valid)) // 下一拍出队数量，由仲裁单元保证合法

  entryNum    := entryNum + numEnq - numDeq
  io.entryNum := entryNum

  val issueAddr = Wire(Vec(2, UInt(log2Up(16).W)))
  for (i <- 0 until 2) issueAddr(i) := Mux(io.issueAddr(i).valid, io.issueAddr(i).bits, 15.U)

  for (i <- 0 until 16) {
    ctrl(i) := MuxCase(
      default = MoveState.stay,
      mapping = Seq(
        (io.enqAddr(0).valid && i.U === io.enqAddr(0).bits) -> MoveState.readFirst,
        (io.enqAddr(1).valid && i.U === io.enqAddr(1).bits) -> MoveState.readSecond,
        (i.U < issueAddr(0))                                -> MoveState.stay,
        (i.U >= issueAddr(0) && i.U < issueAddr(1) - 1.U)   -> MoveState.preFirst,
        (i.U >= issueAddr(1) - 1.U)                         -> MoveState.preSecond
      )
    )
    // io.ctrl(i) := ctrl(i)
  }

  for (i <- 0 until 16) {
    io.dataOut(i) := mem.read(i.U)
  }

}

class IdeaIssueQueue[T <: Data](entryType: T) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Vec(2, DecoupledIO(entryType)))
    val out = Vec(2, DecoupledIO(entryType))
  })

  val compressQueue = Module(new IssueQ(entryType))

  // Select data for issueing
  // just make happy

  // 假设执行单元永远ready(不阻塞)
  val entryNum = compressQueue.io.entryNum

  val numDeq = Mux(entryNum < 2.U, entryNum, 2.U)
  for (i <- 0 until 2) {
    compressQueue.io.issueAddr(i).bits  := i.U
    compressQueue.io.issueAddr(i).valid := Mux(entryNum > i.U, 1.B, 0.B)
    io.out(i).bits                      := compressQueue.io.issueData(i).bits
    io.out(i).valid                     := compressQueue.io.issueData(i).valid
  }

  // 假设此前的单元也永远不阻塞valid = 1

  val numAfterDeq  = entryNum - numDeq
  val numLastSpace = 16.U - numAfterDeq
  val numTryEnq    = PopCount(io.in.map(_.valid))

  val numEnq = Mux(numLastSpace < numTryEnq, numLastSpace, numTryEnq)

  val vaild1 = Mux(numAfterDeq > 15.U, 0.B, 1.B)
  val vaild2 = Mux(numAfterDeq > 14.U, 0.B, 1.B)

  compressQueue.io.enqAddr(0).bits  := numAfterDeq
  compressQueue.io.enqAddr(1).bits  := numAfterDeq + 1.U
  compressQueue.io.enqAddr(0).valid := vaild1
  compressQueue.io.enqAddr(1).valid := vaild2

  compressQueue.io.enqData(0).bits  := io.in(0).bits
  compressQueue.io.enqData(0).valid := io.in(0).valid
  compressQueue.io.enqData(1).bits  := io.in(1).bits
  compressQueue.io.enqData(1).valid := io.in(1).valid

  io.in(0).ready := vaild1
  io.in(1).ready := vaild2
}



class BubbleIssueQueue extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Vec(2, DecoupledIO(new MicroOp)))
    val out = Vec(2, DecoupledIO(new MicroOp))

    val regFileRead   = Flipped(Vec(superscalar, new RegFileReadIO()))
    val regFileWrite = Vec(superscalar, new RegFileWriteIO())
  })

  val compressQueue = Module(new IssueQ(new MicroOp))

  // 默认发射指令从队首两条取出
  for (i <- 0 to 1) compressQueue.io.issueAddr(i).bits := i.U

  val instr    = for (i <- 0 to 1) yield compressQueue.io.issueData(i)
  val instrNum = compressQueue.io.entryNum

  // 绑定out端口
  for (i <- 0 to 1) {
    io.out(i).bits  := instr(i).bits
    io.out(i).valid := instr(i).valid
  }

  // 判断发射指令的条数
  val writingBoard = Mem(32, UInt(4.W)) // writingBoard(i)记录目前执行段有多少条指令正在写入寄存器$i

  /// 在此我们仍假设后面的流水不会阻塞，在后续的版本中会完善decopledIO接口的处理 TODO(1)

  // 对于第一条指令
  when(instrNum === 0.U) {
    // 显然不发射
    compressQueue.io.issueAddr(0).valid := 0.B
  }.otherwise {
    val shamt = instr(0).bits.controlSig.aluXFromShamt
    val rsAddr = instr(0).bits.rsAddr
    val imm = instr(0).bits.controlSig.aluYFromImm
    val rtAddr = instr(0).bits.rtAddr

    val rsRdy = writingBoard(rsAddr) === 0.U
    val rtRdy = writingBoard(rtAddr) === 0.U

    val opRdy = Wire(Bool())
    
    when(imm) {
      // I型指令  rt = rs op imm
      opRdy := rsRdy
    }.otherwise {
      // R型指令  rd = rs op rt
      // 移位指令 rd = rt op shamt 
      when(shamt) {
        opRdy := rtRdy
      }.otherwise {
        opRdy := rsRdy && rtRdy
        // J型指令 TODO(?)
      }
    }

    // 第一条指令是否发射只取决于操作数是否准备完毕
    compressQueue.io.issueAddr(0).valid := opRdy
  }

  // 对于第二条指令
  when(instrNum < 2.U) {
    // 指令不足两条，显然不发射
    compressQueue.io.issueAddr(1).valid := 0.B
  }.otherwise {
    // 检查与前一条指令是否有RAW或WAW，原因：
    // RAW: 必须避免
    // WAW: 将可能出现寄存器写冲突，不能并行执行
    val lastRdAddr = instr(0).bits.writeRegAddr
    val lastWEn = instr(0).bits.controlSig.regWriteEn
    
    val rsAddr = instr(1).bits.rsAddr
    val rsDis = instr(1).bits.controlSig.aluXFromShamt

    val rtAddr = instr(1).bits.rtAddr
    val rtDis = instr(1).bits.controlSig.aluYFromImm
    val rdAddr = instr(1).bits.writeRegAddr
    val wEn = instr(1).bits.controlSig.regWriteEn

    // WAW
    val wAW = lastWEn && wEn && (lastRdAddr === rdAddr)
    // RAW
    val rAW = lastWEn && (rsDis || rsAddr === lastRdAddr) && (rtDis || rtAddr === lastRdAddr)

    // 为了避免访存冲突，所有访存指令默认只能送往EXUnit0执行。后续会进行更改 TODO(2)
    // lw指令loadMode = 1; sw指令stroeMode != disable
    val memUsed = instr(1).bits.controlSig.loadMode || instr(1).bits.controlSig.storeMode =/= StoreMode.disable

    // 同样通过writingBoad判断操作数(可能用到了之前的指令的结果)是否准备完毕
    val rsRdy = rsDis || writingBoard(rsAddr) === 0.U
    val rtRdy = rtDis || writingBoard(rtAddr) === 0.U
    val opNumRdy = rsRdy && rtRdy

    // 第二条指令能够发送的条件: 无WAW RAW 访存,且操作数准备完毕

    val imm = instr(1).bits.controlSig.aluXFromShamt
    val shamt = instr(1).bits.controlSig.aluXFromShamt
    
    val hazard = Wire(Bool())

    when(imm) {
      // I型指令  rt = rs op imm
      when(memUsed) {
        // lw/sw指令
        hazard := 1.B
      }.otherwise {

      }

    }.otherwise{
      // R型指令  rd = rs op rt
      // 移位指令 rd = rt op shamt 
      when(shamt) {

      }.otherwise {

      }
    }
    

  }

}

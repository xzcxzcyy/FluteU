package flute.cp0

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._
import chisel3.stage.ChiselStage

class CP0Read extends Bundle {
  val addr = Input(UInt(5.W))
  val sel  = Input(UInt(3.W))
  val data = Output(UInt(dataWidth.W))
}

class CP0Write extends Bundle {
  val addr   = Input(UInt(5.W))
  val sel    = Input(UInt(3.W))
  val data   = Input(UInt(dataWidth.W))
  val enable = Input(Bool())
}

class CP0DebugIO extends Bundle {
  val badvaddr = Output(UInt(dataWidth.W))
  val count    = Output(UInt(dataWidth.W))
  val status   = Output(UInt(dataWidth.W))
  val cause    = Output(UInt(dataWidth.W))
  val epc      = Output(UInt(dataWidth.W))
  val compare  = Output(UInt(dataWidth.W))
}

class CP0WithCommit extends Bundle {
  val pc         = Input(UInt(addrWidth.W))
  val inSlot     = Input(Bool()) // whether the instruction in pc is delay slot
  val exceptions = Input(new ExceptionBundle)
  val completed  = Input(Bool())
  val eret       = Input(Bool())
}

class CP0WithCore extends Bundle {
  val read    = new CP0Read
  val write   = new CP0Write
  val commit  = new CP0WithCommit
  val intrReq = Output(Bool()) // 例外输出信号
  val epc     = Output(UInt(dataWidth.W))
}

class CP0 extends Module {
  val io = IO(new Bundle {
    val hwIntr = Input(UInt(6.W))
    val core   = new CP0WithCore
    val debug  = new CP0DebugIO
  })

  val badvaddr = new CP0BadVAddr
  val count    = new CP0Count
  val status   = new CP0Status
  val cause    = new CP0Cause
  val epc      = new CP0EPC
  val compare  = new CP0Compare
  val countInc = RegInit(0.B)

  io.debug.badvaddr := badvaddr.reg
  io.debug.count    := count.reg
  io.debug.status   := status.reg.asUInt
  io.debug.cause    := cause.reg.asUInt
  io.debug.epc      := epc.reg
  io.debug.compare  := compare.reg

  val regs = Seq(
    badvaddr,
    count,
    status,
    cause,
    epc,
    compare,
  )
  val readRes = WireInit(0.U(dataWidth.W))
  regs.foreach(r =>
    when(io.core.read.addr === r.addr.U && io.core.read.sel === r.sel.U) {
      readRes := r.reg.asUInt
    }
  )
  io.core.read.data := readRes

  countInc := !countInc

  val commitWire = io.core.commit
  // val completedWire = io.core.commit.completed
  val excVector =
    VecInit(io.core.commit.exceptions.asUInt.asBools.map(_ && io.core.commit.completed))
      .asTypeOf(new ExceptionBundle)

  val hasExc = excVector.asUInt.orR
  val intReqs = for (i <- 0 until 8) yield {
    cause.reg.ip(i) && status.reg.im(i)
  }
  val hasInt = intReqs.foldLeft(0.B)((z, a) => z || a) && status.reg.ie && !status.reg.exl
  val exceptionReqestsNext = 0.B // TODO: 不同类型的异常应当要求从本指令/下一条指令执行
  when(hasInt) {
    epc.reg           := Mux(commitWire.inSlot, commitWire.pc - 4.U, commitWire.pc)
    cause.reg.bd      := commitWire.inSlot && commitWire.completed
    cause.reg.excCode := ExceptionCode.int
    status.reg.exl    := 1.B
  }.elsewhen(hasExc) {
    status.reg.exl := 1.B
    when(!status.reg.exl) {
      cause.reg.bd := commitWire.inSlot
      when(commitWire.inSlot) {
        epc.reg := commitWire.pc - 4.U
      }.elsewhen(exceptionReqestsNext) {
        epc.reg := commitWire.pc + 4.U
      }.otherwise {
        epc.reg := commitWire.pc
      }
    }
    cause.reg.excCode := PriorityMux(
      Seq(
        excVector.adELi -> ExceptionCode.adEL,
        excVector.ri    -> ExceptionCode.ri,
        excVector.ov    -> ExceptionCode.ov,
        excVector.sys   -> ExceptionCode.sys,
        excVector.adELd -> ExceptionCode.adEL,
        excVector.adES  -> ExceptionCode.adEs,
      )
    )
  }
  when(commitWire.eret) {
    status.reg.exl := 0.B
  }

  io.core.intrReq := hasExc || hasInt

  def wReq(r: CP0BaseReg): Bool = {
    io.core.write.enable && io.core.write.addr === r.addr.U && io.core.write.sel === r.sel.U && !hasInt && !hasExc
  }

  // badvaddr

  // count
  when(wReq(count)) {
    count.reg := io.core.write.data
  }.otherwise {
    count.reg := count.reg + countInc.asUInt
  }

  // status
  val writeStatusWire = WireInit(io.core.write.data.asTypeOf(new StatusBundle))
  when(wReq(status)) {
    status.reg.exl := writeStatusWire.exl
    status.reg.ie  := writeStatusWire.ie
    status.reg.im  := writeStatusWire.im
  }

  // cause
  val writeCauseWire = WireInit(io.core.write.data.asTypeOf(new CauseBundle))
  when(wReq(cause)) {
    for (i <- 0 to 1) yield {
      cause.reg.ip(i) := writeCauseWire.ip(i)
    }
  }
  for (i <- 2 to 6) yield {
    cause.reg.ip(i) := io.hwIntr(i - 2)
  }
  when((wReq(count) || wReq(compare)) && !io.hwIntr(5)) {
    cause.reg.ip(7) := 0.B
  }.elsewhen(io.hwIntr(5) || ((count.reg === compare.reg) && (compare.reg =/= 0.U))) {
    cause.reg.ip(7) := 1.B
  }
  when(wReq(count) || wReq(compare)) {
    cause.reg.ti := 0.B
  }.elsewhen(count.reg === compare.reg && compare.reg =/= 0.U) {
    cause.reg.ti := 1.B
  }

  // epc
  when(wReq(epc)) {
    epc.reg := io.core.write.data
  }

  // compare
  when(wReq(compare)) {
    compare.reg := io.core.write.data
  }

  io.core.epc := epc.reg
}

object CP0Main extends App {
  (new ChiselStage).emitVerilog(new CP0, Array("--target-dir", "target/verilog", "--target:fpga"))
}

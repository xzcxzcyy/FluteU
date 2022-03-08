package flute.core.execute.aluexec

import chisel3._
import chisel3.util._

import flute.core.decode.MicroOp
import flute.core.components.RegFileWriteIO
import flute.core.components.ALU
import flute.cache.DCacheIO
import flute.core.decode.{StoreMode, BJCond}
import flute.core.execute.ExecuteFeedbackIO
import flute.config.CPUConfig._
import flute.core.decode.LoadMode

class ALUExecutor extends Module {
  val io = IO(new Bundle {
    val source   = Flipped(DecoupledIO(new MicroOp))
    val dCache   = Flipped(new DCacheIO)
    val regFile  = Flipped(new RegFileWriteIO)
    val feedback = new ExecuteFeedbackIO
  })

  val idEx  = Module(new IdExStage)
  val exMem = Module(new ExMemStage)
  val memWb = Module(new MemWbStage)
  val alu   = Module(new ALU)

  // When not valid, insert a bubble.
  idEx.io.flush  := !io.source.valid
  idEx.io.valid  := 1.B
  exMem.io.valid := 1.B
  memWb.io.valid := 1.B
  exMem.io.flush := 0.B
  memWb.io.flush := 0.B

  io.source.ready := 1.B

  idEx.io.in := io.source.bits

  alu.io.aluOp := idEx.io.data.aluOp
  alu.io.x     := idEx.io.data.op1.op
  alu.io.y     := idEx.io.data.op2.op

  val branchTaken = MuxLookup(
    key = idEx.io.data.bjCond,
    default = 0.B,
    mapping = Seq(
      BJCond.none -> 0.B,
      BJCond.eq   -> alu.io.flag.equal,
      BJCond.ge   -> !alu.io.flag.lessS,
      BJCond.gez  -> !alu.io.flag.lessS, // rs-0
      BJCond.geu  -> !alu.io.flag.lessU,
      BJCond.gt   -> !(alu.io.flag.lessS || alu.io.flag.equal),
      BJCond.gtz  -> !(alu.io.flag.lessS || alu.io.flag.equal),
      BJCond.gtu  -> !(alu.io.flag.lessU || alu.io.flag.equal),
      BJCond.le   -> (alu.io.flag.lessS || alu.io.flag.equal),
      BJCond.lez  -> (alu.io.flag.lessS || alu.io.flag.equal),
      BJCond.leu  -> (alu.io.flag.lessU || alu.io.flag.equal),
      BJCond.lt   -> alu.io.flag.lessS,
      BJCond.ltz  -> alu.io.flag.lessS,
      BJCond.ltu  -> alu.io.flag.lessU,
      BJCond.ne   -> !alu.io.flag.equal,
      BJCond.jr   -> 1.B,
      BJCond.all  -> 0.B,                // HAS been handled by Fetch
    )
  )
  // all 代表 J & JAL, 已经被 Fetch 处理，不需要分支目标地址
  val branchValid = idEx.io.data.bjCond =/= BJCond.none && idEx.io.data.bjCond =/= BJCond.all
  val branchAddr  = WireInit(0.U(addrWidth.W)) // 默认情况下返回0
  when(branchTaken && branchValid) { // 是普通分支指令，且分支成功
    when(idEx.io.data.bjCond === BJCond.jr) {
      branchAddr := idEx.io.data.op1.op
    }.otherwise {
      branchAddr := idEx.io.data.pc + 4.U + Cat(idEx.io.data.immediate, 0.U(2.W))
    }
  }.elsewhen(branchValid) { // 是普通分支指令，但分支失败
    branchAddr := idEx.io.data.pc + 8.U
  }

  exMem.io.in.aluResult          := alu.io.result
  exMem.io.in.writeRegAddr       := idEx.io.data.writeRegAddr
  exMem.io.in.memWriteData       := idEx.io.data.op2.op
  exMem.io.in.control.loadMode   := idEx.io.data.loadMode
  exMem.io.in.control.regWriteEn := idEx.io.data.regWriteEn
  exMem.io.in.control.storeMode  := idEx.io.data.storeMode
  exMem.io.in.branchAddr         := branchAddr
  exMem.io.in.branchValid        := branchValid

  io.feedback.branchAddr.bits  := exMem.io.data.branchAddr
  io.feedback.branchAddr.valid := exMem.io.data.branchValid
  io.dCache.addr               := exMem.io.data.aluResult
  io.dCache.writeData          := exMem.io.data.memWriteData
  io.dCache.storeMode          := exMem.io.data.control.storeMode

  memWb.io.in.aluResult          := exMem.io.data.aluResult
  memWb.io.in.dataFromMem        := io.dCache.readData // TODO: support both lb and lbu
  memWb.io.in.writeRegAddr       := exMem.io.data.writeRegAddr
  memWb.io.in.control.memToReg   := exMem.io.data.control.loadMode =/= LoadMode.disable
  memWb.io.in.control.regWriteEn := exMem.io.data.control.regWriteEn

  io.regFile.writeEnable := memWb.io.data.control.regWriteEn
  io.regFile.writeData := Mux(
    memWb.io.data.control.memToReg,
    memWb.io.data.dataFromMem,
    memWb.io.data.aluResult
  )
  io.regFile.writeAddr := memWb.io.data.writeRegAddr
}

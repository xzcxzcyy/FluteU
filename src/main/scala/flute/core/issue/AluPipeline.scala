package flute.core.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp
import flute.config.CPUConfig._
import flute.core.rob._
import flute.core.components._
import flute.cp0._

class AluWB extends Bundle {
  val rob = new ROBCompleteBundle(robEntryNumWidth)
  val prf = new RegFileWriteIO

  val busyTable = Valid(UInt(phyRegAddrWidth.W))
}

class BypassBundle extends Bundle {
  val in  = Input(Vec(2, UInt(dataWidth.W)))
  val out = Output(UInt(dataWidth.W))
}

class AluPipeline extends Module {
  val io = IO(new Bundle {
    // 无阻塞
    val uop = Input(Valid(new AluEntry))
    val prf = Flipped(new RegFileReadIO)
    val wb  = Output(new AluWB)

    val bypass = new BypassBundle
  })

  /// stage 1: PRF Read & Bypass ---------------------------///
  val readIn = io.uop
  // 操作数3个来源： 立即数,Bypass,PRF
  io.prf.r1Addr := readIn.bits.uop.rsAddr
  io.prf.r2Addr := readIn.bits.uop.rtAddr

  val (op1, op2) =
    AluPipelineUtil.getOp(readIn.bits, Seq(io.prf.r1Data, io.prf.r2Data), io.bypass.in)
  val read2Ex = WireInit(readIn.bits.uop)
  read2Ex.op1.op := op1
  read2Ex.op2.op := op2

  /// stage 2: ALU Execute & BusyTable Checkout  & Bypass Out ///
  val stage2 = Module(new StageReg(Valid(new MicroOp(true))))
  stage2.io.in.bits  := read2Ex
  stage2.io.in.valid := readIn.valid

  val exIn = stage2.io.data

  val alu = Module(new ALU)
  alu.io.aluOp := exIn.bits.aluOp
  alu.io.x     := exIn.bits.op1.op
  alu.io.y     := exIn.bits.op2.op

  // busyTable check out
  io.wb.busyTable.bits  := exIn.bits.writeRegAddr
  io.wb.busyTable.valid := exIn.bits.regWriteEn

  // bypass out
  io.bypass.out := alu.io.result

  val ex2Wb = Wire(new AluExWbBundle)
  ex2Wb.valid    := exIn.valid
  ex2Wb.robAddr  := exIn.bits.robAddr
  ex2Wb.regWEn   := exIn.bits.regWriteEn
  ex2Wb.regWData := alu.io.result
  ex2Wb.regWAddr := exIn.bits.writeRegAddr

  /// stage 3: WriteBack --------------------------------///
  val stage3 = Module(new StageReg(new AluExWbBundle))
  stage3.io.in := ex2Wb

  val wbIn = stage3.io.data

  val rob = AluPipelineUtil.robFromAluExWb(wbIn)
  io.wb.rob             := rob
  io.wb.prf.writeAddr   := wbIn.regWAddr
  io.wb.prf.writeData   := wbIn.regWData
  io.wb.prf.writeEnable := wbIn.regWEn

}

class AluExWbBundle extends Bundle {
  // 需要包含： ALU res, Branch res, Exception res
  val valid     = Bool() // 总体valid: 是否为气泡
  val robAddr   = UInt(robEntryNumWidth.W)
  val exception = new ExceptionBundle
  val regWEn    = Bool()
  val regWData  = UInt(dataWidth.W)
  val regWAddr  = UInt(phyRegAddrWidth.W)
}

object AluPipelineUtil {
  def getOp(aluEntry: AluEntry, prf: Seq[UInt], bypass: Vec[UInt]) = {
    assert(bypass.length == 2 && prf.length == 2)
    val op1 = MuxCase(
      prf(0),
      Seq(
        aluEntry.uop.op1.valid    -> aluEntry.uop.op1.op,
        aluEntry.op1Awaken.awaken -> bypass(aluEntry.op1Awaken.sel)
      )
    )
    val op2 = MuxCase(
      prf(1),
      Seq(
        aluEntry.uop.op2.valid    -> aluEntry.uop.op2.op,
        aluEntry.op2Awaken.awaken -> bypass(aluEntry.op2Awaken.sel)
      )
    )
    (op1, op2)
  }

  def robFromAluExWb(ex2Wb: AluExWbBundle) = {
    val rob = Wire(new ROBCompleteBundle(robEntryNumWidth))
    rob.exception := ex2Wb.exception
    rob.regWData  := ex2Wb.regWData
    rob.robAddr   := ex2Wb.robAddr
    rob.regWEn    := ex2Wb.regWEn
    rob.valid     := ex2Wb.valid

    rob
  }

}

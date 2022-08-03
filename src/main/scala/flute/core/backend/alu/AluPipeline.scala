package flute.core.backend.alu

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.config.CPUConfig._
import flute.core.backend.commit._
import flute.core.components._
import flute.cp0._
import flute.core.components.ALU
import flute.core.backend.decode._

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
    val uop   = Input(Valid(new AluEntry))
    val prf   = Flipped(new RegFileReadIO)
    val wb    = Output(new AluWB)
    val flush = Input(Bool())

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

  /// stage 2: ALU Execute & Branch Compute & BusyTable Checkout  & Bypass Out ///
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
  io.wb.busyTable.valid := exIn.valid && exIn.bits.regWriteEn

  // bypass out
  io.bypass.out := alu.io.result

  // branch
  val (taken, target) = AluPipelineUtil.branchRes(exIn.bits, alu.io.flag)

  val isAndLink = (
    exIn.bits.bjCond === BJCond.bgezal ||
      exIn.bits.bjCond === BJCond.bltzal ||
      exIn.bits.bjCond === BJCond.jal ||
      exIn.bits.bjCond === BJCond.jalr
  )

  val exceptions = WireInit(0.U.asTypeOf(new ExceptionBundle))
  exceptions.bp    := exIn.bits.break
  exceptions.ov    := alu.io.flag.trap
  exceptions.ri    := exIn.bits.reservedI
  exceptions.sys   := exIn.bits.syscall
  exceptions.adELi := (exIn.bits.bjCond =/= BJCond.none) && taken && (target(1, 0) =/= 0.U)

  val ex2Wb = Wire(new AluExWbBundle)
  ex2Wb.valid       := exIn.valid
  ex2Wb.robAddr     := exIn.bits.robAddr
  ex2Wb.regWEn      := exIn.bits.regWriteEn
  ex2Wb.regWData    := Mux(isAndLink, exIn.bits.pc + 8.U, alu.io.result)
  ex2Wb.regWAddr    := exIn.bits.writeRegAddr
  ex2Wb.exception   := exceptions
  ex2Wb.computeBT   := target
  ex2Wb.branchTaken := taken

  /// stage 3: WriteBack --------------------------------///
  val stage3 = Module(new StageReg(new AluExWbBundle))
  stage3.io.in := ex2Wb

  val wbIn = stage3.io.data

  val rob = AluPipelineUtil.robFromAluExWb(wbIn)
  io.wb.rob             := rob
  io.wb.prf.writeAddr   := wbIn.regWAddr
  io.wb.prf.writeData   := wbIn.regWData
  io.wb.prf.writeEnable := wbIn.regWEn

  stage2.io.flush := !readIn.valid || io.flush
  stage3.io.flush := !stage2.io.data.valid || io.flush
  stage2.io.valid := 1.B
  stage3.io.valid := 1.B

}

class AluExWbBundle extends Bundle {
  // 需要包含： ALU res, Branch res, Exception res
  val valid     = Bool() // 总体valid: 是否为气泡
  val robAddr   = UInt(robEntryNumWidth.W)
  val exception = new ExceptionBundle
  val regWEn    = Bool()
  val regWData  = UInt(dataWidth.W)
  val regWAddr  = UInt(phyRegAddrWidth.W)

  val computeBT   = UInt(addrWidth.W)
  val branchTaken = Bool()
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

  def robFromAluExWb(wbIn: AluExWbBundle) = {
    val rob = Wire(new ROBCompleteBundle(robEntryNumWidth))
    rob.exception := wbIn.exception
    rob.regWData  := wbIn.regWData
    rob.robAddr   := wbIn.robAddr
    // rob.regWEn    := ex2Wb.regWEn
    rob.valid    := wbIn.valid
    rob.memWAddr := DontCare
    rob.memWData := DontCare

    rob.branchTaken := wbIn.branchTaken
    rob.computeBT   := wbIn.computeBT
    rob.badvaddr    := wbIn.computeBT
    rob.cp0RegWrite := 0.U.asTypeOf(Valid(UInt(32.W)))
    rob.hiRegWrite  := 0.U.asTypeOf(Valid(UInt(32.W)))
    rob.loRegWrite  := 0.U.asTypeOf(Valid(UInt(32.W)))

    rob
  }

  def branchRes(uop: MicroOp, aluFlag: Flag) = {
    val branchTaken = MuxLookup(
      key = uop.bjCond,
      default = 0.B,
      mapping = Seq(
        BJCond.none   -> 0.B,
        BJCond.beq    -> aluFlag.equal,
        BJCond.bgez   -> !aluFlag.lessS,
        BJCond.bgezal -> !aluFlag.lessS,
        BJCond.bgtz   -> !(aluFlag.lessS || aluFlag.equal),
        BJCond.blez   -> (aluFlag.lessS || aluFlag.equal),
        BJCond.bltz   -> aluFlag.lessS,
        BJCond.bltzal -> aluFlag.lessS,
        BJCond.bne    -> !aluFlag.equal,
        BJCond.j      -> 1.B,
        BJCond.jal    -> 1.B,
        BJCond.jalr   -> 1.B,
        BJCond.jr     -> 1.B,
      )
    )
    // TODO: J & JAL, 已经被 Fetch 处理，给了 taken，且计算地址直接取了 Fetch 给的 PredictBT，如果要改预测的话需要修
    val branchAddr = WireInit(0.U(addrWidth.W)) // 默认情况下返回0

    when(uop.bjCond === BJCond.jr || uop.bjCond === BJCond.jalr) {
      branchAddr := uop.op1.op
    }.elsewhen(uop.bjCond === BJCond.j || uop.bjCond === BJCond.jal) {
      branchAddr := uop.predictBT
    }.otherwise {
      branchAddr := uop.pc + 4.U + Cat(uop.immediate, 0.U(2.W))
    }

    (branchTaken, branchAddr)
  }

}

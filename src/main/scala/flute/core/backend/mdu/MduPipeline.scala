package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.backend.alu.AluEntry
import flute.core.components.HiLoIO
import flute.core.backend.alu.AluWB
import flute.core.backend.alu.AluPipelineUtil
import flute.core.components.RegFileReadIO
import flute.core.backend.decode._
import flute.core.components.StageReg
import flute.core.backend.alu.BypassBundle
import flute.cp0.ExceptionBundle

class MduPipeline extends Module {
	val io = IO(new Bundle {
		val uop  = Input(Valid(new AluEntry))
		val prf  = Flipped(new RegFileReadIO)
		val hilo = Flipped(new HiLoIO)
		val wb   = Output(new AluWB)
		val bypass = new BypassBundle
	})
	
	// Stage 1: PRF Read & Bypass
	val readIn = io.uop
	// 操作数2个来源：Bypass, PRF
	io.prf.r1Addr := readIn.bits.uop.rsAddr
	io.prf.r2Addr := readIn.bits.uop.rtAddr
	val (op1, op2) =
    AluPipelineUtil.getOp(readIn.bits, Seq(io.prf.r1Data, io.prf.r2Data), io.bypass.in)
  val read2Ex = WireInit(readIn.bits.uop)
  read2Ex.op1.op := op1
  read2Ex.op2.op := op2

	// Stage 2: MDU Execute
	val stage2 = Module(new StageReg(Valid(new MicroOp(true))))
	stage2.io.in.bits  := read2Ex 
	stage2.io.in.valid := readIn.valid 

	val exIn = stage2.io.data

	val mdu = Module(new MDU)
	mdu.io.op1 := exIn.bits.op1.op
	mdu.io.op2 := exIn.bits.op2.op 
	mdu.io.md  := MuxLookup(
		exIn.bits.mduOp,
		1.B,
		Seq(
			MDUOp.div   -> 0.B,
			MDUOp.divu  -> 0.B,
			MDUOp.mult  -> 1.B,
			MDUOp.multu -> 1.B
		)
	)
  mdu.io.signed := MuxLookup(
		exIn.bits.mduOp,
		1.B,
		Seq(
			MDUOp.div   -> true.B,
			MDUOp.mult  -> true.B,
			MDUOp.divu  -> false.B,
			MDUOp.multu -> false.B
		)
	)

	val ex2Wb = Wire(new MduExWbBundle)
	ex2Wb.valid := exIn.valid 
	ex2Wb.robAddr := exIn.bits.robAddr
	ex2Wb.hiWEn := true.B
	ex2Wb.hiWData := mdu.io.result.hi 
	ex2Wb.loWEn := true.B 
	ex2Wb.loWData := mdu.io.result.lo 
	ex2Wb.exception := DontCare  // TODO: mdu output有除0 error

	// Stage 3: WriteBack 
	val stage3 = Module(new StageReg(new MduExWbBundle))
	stage3.io.in := ex2Wb 

	val wbIn = stage3.io.data 

}

class MduExWbBundle extends Bundle {
	val valid     = Bool()  // 是否为气泡
	val robAddr   = UInt(robEntryNumWidth.W)
	val exception = new ExceptionBundle 
	val hiWEn     = Bool()
	val hiWData   = UInt(dataWidth.W)
	val loWEn     = Bool()
	val loWData   = UInt(dataWidth.W)
}


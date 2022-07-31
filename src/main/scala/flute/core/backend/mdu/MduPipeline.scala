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
import flute.core.backend.commit.ROBCompleteBundle

class MduPipeline extends Module {
	val io = IO(new Bundle {
		val uop  = Input(Valid(new MicroOp(true)))
		val hilo = Flipped(new HiLoIO)
		val wb   = Output(new AluWB)
	})

	val exIn = io.uop

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
	ex2Wb.valid     := mdu.io.result.hi.valid
	ex2Wb.robAddr   := exIn.bits.robAddr
	ex2Wb.hiWEn     := mdu.io.result.hi.valid
	ex2Wb.hiWData   := mdu.io.result.hi.bits
	ex2Wb.loWEn     := mdu.io.result.lo.valid
	ex2Wb.loWData   := mdu.io.result.lo.bits
	ex2Wb.exception := DontCare  // TODO: mdu output有除0 error

	// Stage 3: WriteBack 
	val stage3 = Module(new StageReg(new MduExWbBundle))
	stage3.io.in := ex2Wb 

	val wbIn = stage3.io.data 
	val writeRob = WireInit(0.U.asTypeOf(new ROBCompleteBundle(robEntryNumWidth)))
	writeRob.valid       := wbIn.valid 
	writeRob.robAddr     := wbIn.robAddr  
	writeRob.exception   := wbIn.exception
	writeRob.memWAddr    := DontCare
	writeRob.memWData    := DontCare 
	writeRob.computeBT   := DontCare  
	writeRob.branchTaken := DontCare
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


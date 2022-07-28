package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.core.components.RegFileReadIO
import flute.core.components.HiLoIO
import flute.core.backend.alu.AluWB

class HiLoPipeline extends Module {
	val io = IO(new Bundle {
		val uop  = Flipped(DecoupledIO(new MicroOp(rename = true)))
		val prf  = Flipped(new RegFileReadIO)
		val hilo = Flipped(new HiLoIO)
		val wb   = Output(new AluWB)
	})

	// Stage 1: PRF Read
	val readIn = io.uop 
	io.prf.r1Addr := readIn.bits.rsAddr
	val op = io.prf.r1Data
	val read2Ex = WireInit(readIn.bits)
	read2Ex.op1.op := op

}

	
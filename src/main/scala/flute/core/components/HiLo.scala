package flute.core.components

import chisel3._
import flute.config.CPUConfig._


class HiLo extends Module {
  val io = IO(new HiLoIO())
	val hilo = RegInit(VecInit(Seq.fill(2)(0.U(dataWidth.W))))
	
	// Read 
	io.read.data := hilo(io.read.hl)

	// Write
	for (i <- 0 until 2) {
		when(io.write(i).enable) {
			hilo(io.write(i).hl) := io.write(i).data
		}
	}
}

class HiLoReadIO extends Bundle {
	val hl   = Input(Bool())
	val data = Output(UInt(dataWidth.W))
}

class HiLoWriteIO extends Bundle {
	val enable = Input(Bool())
	val hl     = Input(Bool())
	val data   = Input(UInt(dataWidth.W))
}

class HiLoIO extends Bundle {
	val read = new HiLoReadIO
	val write = Vec(2, new HiLoWriteIO)
}

object HL {
	val hi = 0.B
	val lo = 1.B
}
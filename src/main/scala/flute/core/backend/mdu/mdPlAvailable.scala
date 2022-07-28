package flute.core.backend.mdu

import chisel3._
import chisel3.util._


class MDPlAvailableIO extends Bundle {
	val available = Output(Bool())
	val write = new Bundle{
		val enable = Input(Bool())
		val value  = Input(Bool())
	}
}

class MDPlAvailable extends Module {
  val io = IO(new MDPlAvailableIO)
    
  val mdPlAvailable = RegInit(true.B)

  io.available := mdPlAvailable
  when(io.write.enable) {
    mdPlAvailable := io.write.value
  }
}

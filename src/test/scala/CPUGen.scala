package core

object CPUGen extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new CPUTop(), args)
}

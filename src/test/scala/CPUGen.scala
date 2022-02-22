package flute

object CPUGen extends App {
    (new chisel3.stage.ChiselStage).emitVerilog(new core.CPUTop(), args)
}
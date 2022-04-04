package flute

import flute.core.CoreTester

object CPUGen extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new CoreTester("sb", "zero.in"), Array("--target-dir", "target/verilog", "--target:fpga"))
}

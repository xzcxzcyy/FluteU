package flute.core.backend.mdu

import chisel3._
import chisel3.util._

class MDUIP extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(DecoupledIO(new MDUIn))
    val res = Output(Valid(new HILORead))

    val flush = Input(Bool())
  })

  val multIp = Module(new MultIp)
  val divIp  = Module(new DivIp)

  io.in.ready := multIp.io.in.ready && divIp.io.in.ready

  multIp.io.in.bits  := io.in.bits
  multIp.io.in.valid := io.in.valid && io.in.bits.mul
  multIp.io.flush    := io.flush

  divIp.io.in.bits  := io.in.bits
  divIp.io.in.valid := io.in.valid && !io.in.bits.mul
  divIp.io.flush    := io.flush

  io.res.bits := MuxCase(
    0.U.asTypeOf(new HILORead),
    Seq(
      multIp.io.res.valid -> multIp.io.res.bits,
      divIp.io.res.valid  -> divIp.io.res.bits,
    )
  )
  io.res.valid := multIp.io.res.valid || divIp.io.res.valid

}

object Gen extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new MDUIP, Array("--target-dir", "target/verilog/mdu", "--target:fpga"))
}

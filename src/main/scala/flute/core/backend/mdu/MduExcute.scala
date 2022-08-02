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

class MduExcute extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new MicroOp(rename = true)))
    val out = Output(new AluWB)
  })

  val uop = io.in.bits

  val mdu = Module(new FakeMDU)

  // Stage 3: WriteBack
  val stage = Module(new StageReg(Valid(new MicroOp(rename = true))))


}

object MduUtil {
  //0 -> hi, 1 -> lo
  def getOp(uop: MicroOp, prfRes: Seq[UInt], hiloRes: Seq[UInt], cp0Res: UInt) = {
    (0.U, 0.U)
  }

  def getRob(uop: MicroOp) = {}
}

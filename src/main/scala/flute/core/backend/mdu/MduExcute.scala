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
    val in = Flipped(Decoupled(new MicroOp(rename = true)))
    val wb = Output(new AluWB)
  })

  val moveExcute    = Module(new MoveExcute)
  val multDivExcute = Module(new MultDivExcute)

  io.in.ready := moveExcute.io.in.ready && multDivExcute.io.in.ready

  moveExcute.io.in.valid    := io.in.valid && MduExcuteUtil.isMove(io.in.bits.mduOp)
  multDivExcute.io.in.valid := io.in.valid && MduExcuteUtil.isMultDiv(io.in.bits.mduOp)

  moveExcute.io.in.bits    := io.in.bits
  multDivExcute.io.in.bits := io.in.bits

  // Stage 3: WriteBack
  val stage = Module(new StageReg(Valid(new MicroOp(rename = true))))

  stage.io.in.valid := moveExcute.io.out.valid || multDivExcute.io.out.valid
  stage.io.in.bits := MuxCase(
    0.U,
    Seq(
      moveExcute.io.out.valid    -> moveExcute.io.out.bits,
      multDivExcute.io.out.valid -> multDivExcute.io.out.bits
    )
  )

  io.wb.rob := MduExcuteUtil.getRobFromUop(stage.io.data.bits, stage.io.data.valid)
	// io.wb.prf
	// io.wb.busyTable

}

// 组合逻辑
class MoveExcute extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new MicroOp(rename = true)))
    val out = ValidIO(new MicroOp(rename = true))
  })
	io.in.ready := 1.B
}

class MultDivExcute extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new MicroOp(rename = true)))
    val out = ValidIO(new MicroOp(rename = true))
  })

  val mdu = Module(new FakeMDU)
}

object MduExcuteUtil {
  def isMove(mduOp: UInt) = {
    require(mduOp.getWidth == MDUOp.width)

    (mduOp === MDUOp.mfc0 || mduOp === MDUOp.mtc0
    || mduOp === MDUOp.mfhi || mduOp === MDUOp.mthi
    || mduOp === MDUOp.mflo || mduOp === MDUOp.mtlo)
  }

  def isMultDiv(mduOp: UInt) = {
    require(mduOp.getWidth == MDUOp.width)

    (mduOp === MDUOp.mult || mduOp === MDUOp.multu
    || mduOp === MDUOp.div || mduOp === MDUOp.divu)
  }

  def getRobFromUop(uop: MicroOp, valid: Bool) = {
    val rob = Wire(new ROBCompleteBundle(robEntryNumWidth))

    rob
  }
}

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
import flute.cp0.CP0Read

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
  val stage = Module(new StageReg(Valid(new MduWB)))

  stage.io.in.valid := moveExcute.io.out.valid || multDivExcute.io.out.valid
  stage.io.in.bits := MuxCase(
    0.U,
    Seq(
      moveExcute.io.out.valid    -> moveExcute.io.out.bits,
      multDivExcute.io.out.valid -> multDivExcute.io.out.bits
    )
  )

  io.wb.rob             := stage.io.data.bits.rob
  io.wb.prf.writeAddr   := stage.io.data.bits.prf.writeAddr
  io.wb.prf.writeData   := stage.io.data.bits.prf.writeData
  io.wb.prf.writeEnable := stage.io.data.bits.prf.writeEnable
  io.wb.busyTable       := stage.io.data.bits.busyTable
}

class MduWB extends Bundle {
  val rob = new ROBCompleteBundle(robEntryNumWidth)
  val prf = new Bundle {
    val writeAddr   = UInt(phyRegAddrWidth.W)
    val writeData   = UInt(dataWidth.W)
    val writeEnable = Bool()
  }
  val busyTable = Valid(UInt(phyRegAddrWidth.W))
}

// 组合逻辑
class MoveExcute extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new MicroOp(rename = true)))
    val out = ValidIO(new MduWB)

    val hilo = Input(new HILORead)
    val cp0  = Flipped(new CP0Read)
  })
  io.in.ready := 1.B

  val uop = io.in.bits

  // cp0
  io.cp0.addr := io.in.bits.cp0RegAddr
  io.cp0.sel  := io.in.bits.cp0RegSel

  val wb          = WireInit(0.U.asTypeOf(new MduWB))
  val robComplete = WireInit(0.U.asTypeOf(new ROBCompleteBundle))

  val regWData = MuxLookup(
    io.in.bits.mduOp,
    0.U,
    Seq(
      MDUOp.mfhi -> io.hilo.hi,
      MDUOp.mflo -> io.hilo.lo,
      MDUOp.mfc0 -> io.cp0.data
    )
  )

  robComplete.regWData := regWData

  robComplete.hiRegWrite.bits  := uop.op1.op
  robComplete.hiRegWrite.valid := uop.mduOp === MDUOp.mthi

  robComplete.loRegWrite.bits  := uop.op1.op
  robComplete.loRegWrite.valid := uop.mduOp === MDUOp.mtlo

  robComplete.cp0RegWrite.bits  := uop.op2.op
  robComplete.cp0RegWrite.valid := uop.mduOp === MDUOp.mtc0

  wb.rob             := robComplete
  wb.prf.writeEnable := uop.regWriteEn
  wb.prf.writeAddr   := uop.writeRegAddr
  wb.prf.writeData   := regWData
  wb.busyTable.valid := uop.regWriteEn
  wb.busyTable.bits  := uop.writeRegAddr
}

class MultDivExcute extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new MicroOp(rename = true)))
    val out = ValidIO(new MduWB)
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

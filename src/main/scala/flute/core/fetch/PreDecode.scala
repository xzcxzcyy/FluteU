package flute.core.fetch

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._
import flute.config.BasicInstructions
import flute.util.BitPatCombine

class PreDecode extends Module {

  private object Inst extends BasicInstructions {
    def needUpdate = BitPatCombine(Seq(J, JAL))
    def needWait = BitPatCombine(
      Seq(
        BEQ,
        BGEZ,
        BGEZAL,
        BGTZ,
        BLEZ,
        BLTZ,
        BLTZAL,
        BNE,
        JALR,
        JR,
      )
    )
  }

  val io = IO(new Bundle {
    val instruction = Flipped(ValidIO(UInt(instrWidth.W)))
    val pc          = Input(UInt(addrWidth.W))
    val stallReq    = Output(Bool())
    val targetAddr  = ValidIO(UInt(instrWidth.W))
  })

  val pcplusfour = io.pc + 4.U

  when(io.instruction.valid) {
    when(Inst.needUpdate(io.instruction.bits)) {
      io.targetAddr.bits  := Cat(pcplusfour(31, 28), io.instruction.bits(25, 0), 0.U(2.W))
      io.targetAddr.valid := 1.B
      io.stallReq         := 0.B
    }.elsewhen(Inst.needWait(io.instruction.bits)) {
      io.targetAddr.valid := 0.B
      io.targetAddr.bits  := DontCare
      io.stallReq         := 1.B
    }.otherwise {
      io.targetAddr.valid := 0.B
      io.targetAddr.bits  := DontCare
      io.stallReq         := 0.B
    }
  }.otherwise {
    io.stallReq         := 0.B
    io.targetAddr.bits  := DontCare
    io.targetAddr.valid := 0.B
  }
}

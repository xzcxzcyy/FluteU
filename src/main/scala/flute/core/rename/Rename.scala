package flute.core.rename

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.decode.DecodeIO
import flute.core.decode.MicroOp
import flute.core.rename.freelist.FreeList
import flute.core.components.RegFile

// the io that with next
class RenameIO extends Bundle {
  val microOps = Vec(superscalar, DecoupledIO(new MicroOp))
  val robidx   = Vec(superscalar, DecoupledIO(UInt(PhyRegIdxWidth.W)))
}

class RenameFeedbackIO extends Bundle{
  val stall = Bool()
}

class RenameBundle extends Bundle {
  // from decode
  val in = Flipped(new DecodeIO)
  // wrapped in microOps
  val out = Output(new RenameIO)
  val feedback = Output(new RenameFeedbackIO)
}

class Rename extends Module {
  val io = new RenameBundle

  val rat      = new RAT
  val freelist = new FreeList
  val regfile  = new RegFile(superscalar * 2, superscalar)

  val uops = Wire(Vec(superscalar, new MicroOp))
  for (i <- 0 until superscalar) {
    uops(i).regWriteEn   := io.in.microOps(i).bits.regWriteEn
    uops(i).loadMode     := io.in.microOps(i).bits.loadMode
    uops(i).storeMode    := io.in.microOps(i).bits.storeMode
    uops(i).aluOp        := io.in.microOps(i).bits.aluOp
    uops(i).op1          <> io.in.microOps(i).bits.op1
    uops(i).op2          <> io.in.microOps(i).bits.op2
    uops(i).bjCond       := io.in.microOps(i).bits.bjCond
    uops(i).writeRegAddr := DontCare
    uops(i).immediate    := io.in.microOps(i).bits.immediate
    uops(i).rsAddr       := DontCare
    uops(i).rtAddr       := DontCare
    uops(i).pc           := io.in.microOps(i).bits.pc
  }

  for (i <- 0 until superscalar) {
    val microOp = io.in.microOps(i)
    when(microOp.valid) {
      // judge whether has dest or ont
      when(microOp.bits.regWriteEn) {
        // give rob the idx that dest used last in rat
        io.out.robidx(i).valid      := 1.B
        rat.io.ReadPorts(i)(2).ready := 1.B
        when(rat.io.ReadPorts(i)(2).valid){ rat.io.ReadPorts(i)(2).bits.addr := microOp.bits.writeRegAddr}
        io.out.robidx(i).bits       := rat.io.ReadPorts(i)(2).bits.data

        // designate a new phyreg from free list
        freelist.io.read(i).ready := 1.B
        when(freelist.io.read(i).valid){
          uops(i).writeRegAddr := freelist.io.read(i).bits
          // put the mapping connection into rat
          rat.io.WritePorts(i).wen := 1.B
          rat.io.WritePorts(i).addr := microOp.bits.regWriteEn
          rat.io.WritePorts(i).data := uops(i).writeRegAddr
        }
      }.otherwise {
        io.out.robidx(i).valid      := 0.B
        rat.io.ReadPorts(i)(2).ready := 0.B

        freelist.io.read(i).ready := 0.B
        rat.io.WritePorts(i).wen := 0.B
        uops(i).writeRegAddr := microOp.bits.writeRegAddr
      }

      when(!microOp.bits.op1.valid){
        // the instruction has rs
        rat.io.ReadPorts(i)(0).ready := 1.B
        rat.io.ReadPorts(i)(0).bits.addr := microOp.bits.rsAddr
        when(rat.io.ReadPorts(i)(0).valid){
          uops(i).rsAddr := rat.io.ReadPorts(i)(0).bits.data
        }
      }.otherwise{
        rat.io.ReadPorts(i)(0).ready := 0.B
        uops(i).rsAddr := microOp.bits.rsAddr
      }
      when(!microOp.bits.op2.valid){
        // the instruction has rt
        rat.io.ReadPorts(i)(1).ready := 1.B
        rat.io.ReadPorts(i)(1).bits.addr := microOp.bits.rtAddr
        when(rat.io.ReadPorts(i)(1).valid){
          uops(i).rtAddr := rat.io.ReadPorts(i)(1).bits.data
        }
      }.otherwise{
        rat.io.ReadPorts(i)(1).ready := 0.B
        uops(i).rtAddr := microOp.bits.rtAddr
      }
    }
    //TODO what's ready in microOps means? and how to evaluate
    io.out.microOps(i) <> uops(i)
  }
  io.feedback.stall := freelist.io.stall
}

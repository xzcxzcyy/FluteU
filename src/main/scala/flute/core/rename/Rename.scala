package flute.core.rename

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.decode.DecodeIO
import flute.core.decode.MicroOp
import flute.core.rename.freelist.FreeList
import flute.core.components.RegFile

class RenameIO extends Bundle {
  val microOps = Vec(superscalar, DecoupledIO(new MicroOp))
  val robidx   = Vec(superscalar, DecoupledIO(UInt(phyRegAddrWidth.W)))
}

class RenameFeedbackIO extends Bundle {}

class RenameBundle extends Bundle {
  // from decode
  val in = Flipped(new DecodeIO)
  // wrapped in microOps
  val out = Output(new RenameIO)
}

class Rename extends Module {
  val io = new RenameBundle

  val rat      = new RAT
  val freelist = new FreeList
  val regfile  = new RegFile(superscalar * 2, superscalar)

  //init
  for (i <- 0 until superscalar) {
    io.in.microOps(i).ready  := 0.B
    io.out.microOps(i).bits  := DontCare
    io.out.microOps(i).valid := 0.B

    io.out.robidx(i).valid := 0.B
    io.out.robidx(i).bits  := DontCare

    rat.io.ReadPorts(i)(0).ready := 0.B
    rat.io.ReadPorts(i)(1).ready := 0.B
    rat.io.ReadPorts(i)(2).ready := 0.B
    rat.io.ReadPorts(i)(0).addr  := DontCare
    rat.io.ReadPorts(i)(1).addr  := DontCare
    rat.io.ReadPorts(i)(2).addr  := DontCare

    rat.io.WritePorts(i).wen  := 0.B
    rat.io.WritePorts(i).addr := DontCare
    rat.io.WritePorts(i).data := DontCare

    freelist.io.read(i).ready  := 0.B
    freelist.io.write(i).bits  := DontCare
    freelist.io.write(i).valid := 0.B

    // regfile留着以后探索吧
  }

  val uops = Wire(Vec(superscalar, new MicroOp))
  for (i <- 0 until superscalar) {
    uops(i).regWriteEn := io.in.microOps(i).bits.regWriteEn
    uops(i).loadMode   := io.in.microOps(i).bits.loadMode
    uops(i).storeMode  := io.in.microOps(i).bits.storeMode
    uops(i).aluOp      := io.in.microOps(i).bits.aluOp
    uops(i).op1 <> io.in.microOps(i).bits.op1
    uops(i).op2 <> io.in.microOps(i).bits.op2
    uops(i).bjCond       := io.in.microOps(i).bits.bjCond
    uops(i).writeRegAddr := DontCare
    uops(i).immediate    := io.in.microOps(i).bits.immediate
    uops(i).rsAddr       := DontCare
    uops(i).rtAddr       := DontCare
    uops(i).pc           := io.in.microOps(i).bits.pc
  }
  // check RAW hazard
  val rsLRecipe       = Wire(Vec(superscalar, UInt(log2Up(superscalar).W)))
  val rsRRecipe       = Wire(Vec(superscalar, UInt(log2Up(superscalar).W)))
  val usedWriteRecipe = Wire(Vec(superscalar, UInt(log2Up(superscalar).W)))
  for (i <- 0 until superscalar) { // default from rat
    rsLRecipe(i)       := i.U
    rsRRecipe(i)       := i.U
    usedWriteRecipe(i) := i.U
  }

  for (i <- 1 until superscalar; j <- 0 until i) {
    val microOpNow = io.in.microOps(i)
    val microOpPre = io.in.microOps(j)
    when(microOpNow.bits.rsAddr === microOpPre.bits.writeRegAddr) {
      rsLRecipe(i) := j.U
    }
    when(microOpNow.bits.rtAddr === microOpPre.bits.writeRegAddr) {
      rsRRecipe(i) := j.U
    }
    when(microOpNow.bits.writeRegAddr === microOpPre.bits.writeRegAddr){
      usedWriteRecipe(i) := j.U
    }
  }
  // check WAW hazard
  for (i <- 0 until superscalar) {
    rat.io.WritePorts(i).wen := 1.B
  }
  for (i <- 0 until superscalar; j <- i + 1 until superscalar) {
    val microOpNow = io.in.microOps(i)
    val microOpAft = io.in.microOps(j)
    when(microOpNow.bits.writeRegAddr === microOpAft.bits.writeRegAddr) {
      rat.io.WritePorts(i).wen := 0.B
    }
  }

  // ideal rename without conflict
  for (i <- 0 until superscalar) {
    val microOp = io.in.microOps(i)
    when(microOp.valid) {
      // judge whether has dest or ont
      when(microOp.bits.regWriteEn) {
        // give rob the idx that dest used last in rat
        io.out.robidx(i).valid       := 1.B
        rat.io.ReadPorts(i)(2).ready := usedWriteRecipe(i) === i.U
        rat.io.ReadPorts(i)(2).addr  := microOp.bits.writeRegAddr
        when(rat.io.ReadPorts(i)(2).valid) {
          io.out.robidx(i).bits := rat.io.ReadPorts(i)(2).data
        }.otherwise{
          io.out.robidx(i).bits := uops(usedWriteRecipe(i).litValue.intValue).writeRegAddr
        }
        // designate a new phyreg from free list
        freelist.io.read(i).ready := 1.B
        when(freelist.io.read(i).valid) {
          uops(i).writeRegAddr := freelist.io.read(i).bits
          // put the mapping connection into rat
          // rat.io.WritePorts(i).wen := 1.B
          rat.io.WritePorts(i).addr := microOp.bits.regWriteEn
          rat.io.WritePorts(i).data := uops(i).writeRegAddr
        }
      }.otherwise {
        uops(i).writeRegAddr := microOp.bits.writeRegAddr
      }

      // the instruction has rs
      when(!microOp.bits.op1.valid) {
        rat.io.ReadPorts(i)(0).ready := rsLRecipe(i) === i.U
        rat.io.ReadPorts(i)(0).addr  := microOp.bits.rsAddr
        when(rat.io.ReadPorts(i)(0).valid) {
          uops(i).rsAddr := rat.io.ReadPorts(i)(0).data
        }.otherwise {
          uops(i).rsAddr := uops(rsLRecipe(i).litValue.intValue).writeRegAddr
        }
      }.otherwise {
        uops(i).rsAddr := microOp.bits.rsAddr
      }

      // the instruction has rt
      when(!microOp.bits.op2.valid) {
        rat.io.ReadPorts(i)(1).ready := rsRRecipe(i) === i.U
        rat.io.ReadPorts(i)(1).addr  := microOp.bits.rtAddr
        when(rat.io.ReadPorts(i)(1).valid) {
          uops(i).rtAddr := rat.io.ReadPorts(i)(1).data
        }.otherwise {
          uops(i).rtAddr := uops(rsRRecipe(i).litValue.intValue).writeRegAddr
        }
      }.otherwise {
        uops(i).rtAddr := microOp.bits.rtAddr
      }
    }
    io.out.microOps(i) <> uops(i)
    io.in.microOps(i).ready := !freelist.io.stall && io.out.microOps(i).ready
  }
}
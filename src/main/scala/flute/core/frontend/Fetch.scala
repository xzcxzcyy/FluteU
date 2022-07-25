package flute.core.frontend

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._

import flute.config.CPUConfig._
import flute.cache.ICacheIO
import flute.util.BitMode.fromIntToBitModeLong
import flute.util.ValidBundle

class ExecuteFeedbackIO extends Bundle {
  val branchAddr = Valid(UInt(instrWidth.W))
}

class FetchIO extends Bundle {
  val ibufferEntries = Vec(decodeWay, Decoupled(new IBEntry))
}

class IBEntry extends Bundle {
  val inst = UInt(instrWidth.W)
  val addr = UInt(addrWidth.W)

  val predictBT = UInt(addrWidth.W)
}

class FetchWithCP0 extends Bundle {
  val intrReq = Input(Bool())
  val epc     = Input(UInt(dataWidth.W))
}

class Fetch extends Module {
  val io = IO(new Bundle {
    val withDecode         = new FetchIO()
    val feedbackFromExec   = Flipped(new ExecuteFeedbackIO())
    val iCache             = Flipped(new ICacheIO())
    val cp0                = new FetchWithCP0
    val pc                 = Output(UInt(addrWidth.W))
    val state              = Output(UInt(State.width.W))
  })

  private object State {
    val width         = 2
    val Free          = 0.U(width.W)
    val FirstAndBlock = 1.U(width.W)
    val Blocked       = 2.U(width.W)
    val RESERVED      = 3.U(width.W) // illegal state; do not use
  }

  val pc          = Module(new PC)
  val ibuffer     = Module(new Ibuffer(new IBEntry, 16, decodeWay, fetchGroupSize))
  val preDecoders = for (i <- 0 until fetchGroupSize) yield Module(new PreDecode)
  val branchAddr  = RegInit(0.U.asTypeOf(ValidBundle(UInt(addrWidth.W))))
  val state       = RegInit(State.Free)

  ibuffer.io.flush := io.cp0.intrReq

  val nextState = Wire(UInt(State.width.W))
  val nextPc    = Wire(UInt(addrWidth.W))

  nextPc    := DontCare
  nextState := DontCare

  val bias   = pc.io.out(1 + fetchGroupWidth, 1 + 1)
  val pdOuts = Wire(Vec(fetchGroupSize, new PreDecoderOutput))

  for (i <- 0 until fetchGroupSize) yield pdOuts(i) := preDecoders(i).io.out

  for (i <- 0 until fetchGroupSize) {
    preDecoders(i).io.instruction.bits  := io.iCache.data.bits(i)
    preDecoders(i).io.instruction.valid := io.iCache.data.valid && (i.U >= bias)
    preDecoders(i).io.pc := Cat(
      pc.io.out(31, fetchGroupWidth + 2),
      i.U(fetchGroupWidth.W),
      0.U(2.W)
    )
  }

  val earliestBranchInd = PriorityEncoder(pdOuts.map(_.isBranch))

  for (i <- 0 until fetchGroupSize) {
    val instAddr = Cat(
      pc.io.out(31, fetchGroupWidth + 2),
      i.U(fetchGroupWidth.W),
      0.U(2.W)
    )
    ibuffer.io.write(i).bits.addr := instAddr
    // TODO: Here we make up a branch fail.
    ibuffer.io.write(i).bits.predictBT := -1.BM.U
    ibuffer.io.write(i).bits.inst := io.iCache.data.bits(i)
    when(state === State.Free) {
      ibuffer.io.write(i).valid :=
        io.iCache.data.valid && (i.U >= bias) && (i.U <= earliestBranchInd)
    }.elsewhen(state === State.FirstAndBlock) {
      ibuffer.io.write(i).valid := io.iCache.data.valid && (i.U === bias)
    }.otherwise { // State.{Blocked, RESERVED}
      ibuffer.io.write(i).valid := 0.B
    }
  }
  val numEnq = PopCount(ibuffer.io.write.map(_.ready))

  switch(state) {
    is(State.Free) {
      val lastInstInd = bias + numEnq - 1.U
      when(numEnq > 0.U && pdOuts(lastInstInd).isBranch) {
        when(pdOuts(lastInstInd).targetAddr.valid) {
          branchAddr.bits  := pdOuts(lastInstInd).targetAddr.bits
          branchAddr.valid := pdOuts(lastInstInd).targetAddr.valid
        }
        nextState := State.FirstAndBlock
      }.otherwise {
        nextState := State.Free
      }
      nextPc := pc.io.out + Cat(numEnq, 0.U(2.W))
    }

    is(State.FirstAndBlock) {
      when(numEnq === 0.U) {
        nextState := State.FirstAndBlock
        nextPc    := pc.io.out
        when(io.feedbackFromExec.branchAddr.valid) {
          branchAddr.valid := io.feedbackFromExec.branchAddr.valid
          branchAddr.bits  := io.feedbackFromExec.branchAddr.bits
        }
      }.otherwise {
        when(branchAddr.valid) {
          nextPc           := branchAddr.bits
          branchAddr.valid := 0.B
          nextState        := State.Free
        }.elsewhen(io.feedbackFromExec.branchAddr.valid) {
          nextPc    := io.feedbackFromExec.branchAddr.bits
          nextState := State.Free
        }.otherwise {
          nextPc    := pc.io.out
          nextState := State.Blocked
        }
      }
    }

    is(State.Blocked) {
      when(io.feedbackFromExec.branchAddr.valid) {
        nextState := State.Free
        nextPc    := io.feedbackFromExec.branchAddr.bits
      }.elsewhen(branchAddr.valid) {
        nextState        := State.Free
        nextPc           := branchAddr.bits
        branchAddr.valid := 0.B
      }.otherwise {
        nextPc    := pc.io.out
        nextState := State.Blocked
      }
    }

    is(State.RESERVED) {
      nextPc    := pc.io.out
      nextState := State.RESERVED
    }
  }

  state := nextState

  pc.io.stall := 0.B
  pc.io.in    := nextPc

  io.iCache.addr.bits  := pc.io.out
  io.iCache.addr.valid := 1.B
  io.iCache.data.ready := 1.B

  io.withDecode.ibufferEntries <> ibuffer.io.read

  // debug in verilog
  io.pc    := pc.io.out
  io.state := state
}

class PC extends Module {
  val io = IO(new Bundle {
    val out   = Output(UInt(addrWidth.W))
    val in    = Input(UInt(addrWidth.W))
    val stall = Input(Bool())
  })
  val data = RegInit(0.U(addrWidth.W))
  when(!io.stall) {
    data := io.in
  }
  io.out := data
}

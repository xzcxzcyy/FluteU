package flute.core.fetch

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._

import flute.config.CPUConfig._
import flute.cache.ICacheIO
import flute.core.execute.ExecuteFeedbackIO
import flute.core.decode.DecodeFeedbackIO

class FetchIO extends Bundle {
  val insts       = Output(Vec(fetchGroupSize, new IBEntry)) // 指令队列
  val instNum     = Output(UInt(fetchAmountWidth.W))         // 队列指令数量
  val willProcess = Input(UInt(fetchAmountWidth.W))          // 下一拍到来时，decode取走指令条数
}

class IBEntry extends Bundle {
  val inst = UInt(instrWidth.W)
  val addr = UInt(addrWidth.W)
}

class Fetch extends Module {
  val io = IO(new Bundle {
    val withDecode         = new FetchIO()
    val feedbackFromDecode = Flipped(new DecodeFeedbackIO())
    val feedbackFromExec   = Flipped(new ExecuteFeedbackIO())
    val iCache             = Flipped(new ICacheIO())
    val state              = Output(UInt(State.width.W))
  })

  private object State {
    val width         = 2
    val Free          = 0.U(width.W)
    val FirstAndBlock = 1.U(width.W)
    val Blocked       = 2.U(width.W)
    val RESERVED      = 3.U(width.W)
  }

  val instNum     = RegInit(0.U(fetchAmountWidth.W))
  val pc          = Module(new PC)
  val iB          = RegInit(VecInit(Seq.fill(fetchGroupSize)((new IBEntry).Lit())))
  val preDecoders = for (i <- 0 until fetchGroupSize) yield Module(new PreDecode)
  val branchAddr = RegInit(
    (new Bundle {
      val valid = Bool()
      val bits  = UInt(addrWidth.W)
    }).Lit()
  )
  val state = RegInit(State.Free)

  val nextState     = Wire(UInt(State.width.W))
  val nextPc        = Wire(UInt(addrWidth.W))
  val instNumInc    = Wire(UInt(fetchAmountWidth.W))

  instNumInc := DontCare
  nextPc     := DontCare
  nextState  := DontCare

  val bias          = pc.io.out(1 + fetchGroupWidth, 1 + 1)
  val preDecoderIOs = Wire(Vec(fetchGroupSize, new PreDecodeOutput))

  io.state := state

  for (i <- 0 until fetchGroupSize) yield preDecoderIOs(i) := preDecoders(i).io.out

  for (i <- 0 until fetchGroupSize) {
    preDecoders(i).io.instruction.bits  := io.iCache.data.bits(i)
    preDecoders(i).io.instruction.valid := io.iCache.data.valid && (i.U >= bias)
    preDecoders(i).io.pc := Cat(
      pc.io.out(31, fetchGroupWidth + 2),
      i.U(fetchGroupWidth.W),
      0.U(2.W)
    )
  }

  val earliestBranchInd = PriorityEncoder(preDecoderIOs.map(_.isBranch))

  val instNumIBPermits = fetchGroupSize.U - instNum + io.withDecode.willProcess //四位
  val instNumCacheLineHas = Mux(
    io.iCache.data.valid,
    earliestBranchInd - bias + 1.U(fetchAmountWidth.W),
    0.U(fetchAmountWidth.W)
  ) //四位

  switch(state) {
    is(State.Free) {
      instNumInc := Mux(
        instNumIBPermits <= instNumCacheLineHas,
        instNumIBPermits,
        instNumCacheLineHas
      )
      val lastInstInd = bias + instNumInc - 1.U
      when(instNumInc > 0.U && preDecoderIOs(lastInstInd).isBranch) {
        when(preDecoderIOs(lastInstInd).targetAddr.valid) {
          branchAddr := preDecoderIOs(lastInstInd).targetAddr
        }
        nextState := State.FirstAndBlock
      }.otherwise {
        nextState := State.Free
      }
      nextPc := pc.io.out + Cat(instNumInc, 0.U(2.W))
    }

    is(State.FirstAndBlock) {
      when(!io.iCache.data.valid) {
        nextState  := State.FirstAndBlock
        nextPc     := pc.io.out
        instNumInc := 0.U
        when(io.feedbackFromExec.branchAddr.valid) {
          branchAddr := io.feedbackFromExec.branchAddr
        }
      }.otherwise {
        instNumInc := Mux(
          instNumIBPermits > 1.U,
          1.U,
          instNumIBPermits
        )
        when(instNumInc > 0.U) {
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
        }.otherwise {
          when(io.feedbackFromExec.branchAddr.valid) {
            branchAddr := io.feedbackFromExec.branchAddr
          }
          nextPc    := pc.io.out
          nextState := State.FirstAndBlock
        }
      }
    }

    is(State.Blocked) {
      instNumInc := 0.U
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

    // is(State.RESERVED) {
    //   instNumInc := DontCare
    //   nextPc     := DontCare
    //   nextState  := DontCare
    // }
  }

  for (i <- 0 until fetchGroupSize) yield {
    when(i.U + io.withDecode.willProcess < instNum) {
      iB(i.U) := iB(i.U + io.withDecode.willProcess)
    }.elsewhen(i.U + io.withDecode.willProcess - instNum < instNumInc) {
      iB(i.U).inst := io.iCache.data.bits(
        i.U + io.withDecode.willProcess - instNum + bias
      )
      iB(i.U).addr := Cat(
        pc.io.out(31, fetchGroupWidth + 2),
        (i.U + io.withDecode.willProcess - instNum + bias)(fetchGroupWidth - 1, 0),
        0.U(2.W)
      )
    }.otherwise {
      iB(i.U) := (new IBEntry).Lit()
    }
  }

  state := nextState

  instNum := instNum - io.withDecode.willProcess + instNumInc

  pc.io.stall := 0.B
  pc.io.in    := nextPc

  io.iCache.addr.bits  := pc.io.out
  io.iCache.addr.valid := 1.B
  io.iCache.data.ready := 1.B

  io.withDecode.instNum := instNum
  io.withDecode.insts   := iB
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

package flute.core.rob

import chisel3._
import chisel3.util._
import flute.core.decode.StoreMode
import flute.core.rename.RenameCommit
import flute.config.CPUConfig._
import flute.cp0.CP0WithCommit
import flute.cache.top.DCacheReq

class StoreCommit extends Bundle {}

class BranchTrain extends Bundle {
  val pc     = UInt(addrWidth.W) // 指令pc
  val taken  = Bool()            // 是否跳转
  val target = UInt(instrWidth.W)
}

class BranchCommit extends Bundle {
  val train     = Valid(new BranchTrain)
  val pcRestore = Valid(UInt(addrWidth.W))
}

class Commit(nCommit: Int = 2) extends Module {
  assert(nCommit == 2)

  val io = IO(new Bundle {
    val rob = Vec(nCommit, Flipped(Decoupled(new ROBEntry)))

    val commit = Flipped(new RenameCommit(nCommit))

    val store  = Output(new StoreCommit)
    val branch = Output(new BranchCommit)

    val cp0     = Flipped(new CP0WithCommit)
    val intrReq = Input(Bool())

    val dCache = DecoupledIO(new DCacheReq)

    val recover = Output(Bool())
  })

  val robRaw = io.rob.map(r => r.bits)

  val validMask = WireDefault(VecInit(io.rob.map(r => r.valid)))

  val isStore = WireInit(VecInit(robRaw.map(r => r.memWMode =/= StoreMode.disable)))

  val completeMask = Wire(Vec(nCommit, Bool()))

  for (i <- 0 until nCommit) {
    var complete = robRaw(i).complete && (!isStore(i) || io.dCache.ready)
    for (j <- 0 until i) {
      complete = complete && robRaw(j).complete && (!isStore(i) || io.dCache.ready)
    }
    completeMask(i) := complete
  }

  val storeMask = Wire(Vec(nCommit, Bool()))

  for (i <- 0 until nCommit) {
    var hasNoStoreBefore = 1.B
    for (j <- 0 until i) {
      hasNoStoreBefore = hasNoStoreBefore && !isStore(j)
    }
    storeMask(i) := !isStore(i) || hasNoStoreBefore
  }

  val cp0Mask = WireInit(VecInit(Seq.fill(nCommit)(!io.intrReq)))

  val programException = Wire(Vec(nCommit, Bool()))
  val branchFail       = Wire(Vec(nCommit, Bool()))

  for (i <- 0 until nCommit) {
    programException(i) := robRaw(i).exception.asUInt.orR
    branchFail(i)       := robRaw(i).branch && robRaw(i).predictBT =/= robRaw(i).computeBT
  }
  val hasExceptionOrFail = Wire(Vec(2, Bool()))

  hasExceptionOrFail(0) := 0.B
  hasExceptionOrFail(1) := programException(1) || branchFail(1)

  val mask1 = WireDefault(
    VecInit(
      (0 until nCommit).map(i =>
        validMask(i) && completeMask(i) && storeMask(i) && !hasExceptionOrFail(i) && cp0Mask(i)
      )
    )
  )

  val cycleEn = WireInit(1.B)

  when(branchFail(0) && !completeMask(1)) {
    cycleEn := 0.B
  }

  // [[io.commit]] 数据通路
  for (i <- 0 until nCommit) {
    io.commit.rmt.write(i).addr      := robRaw(i).logicReg
    io.commit.rmt.write(i).data      := robRaw(i).physicReg
    io.commit.freelist.free(i).bits  := robRaw(i).originReg
    io.commit.freelist.alloc(i).bits := robRaw(i).physicReg
    val wbValid = cycleEn && mask1(i) && robRaw(i).regWEn
    io.commit.rmt.write(i).en         := wbValid
    io.commit.freelist.free(i).valid  := wbValid
    io.commit.freelist.alloc(i).valid := wbValid
  }

  // // Rename Commit
  // for (i <- 0 until nCommit) {
  //   // [[io.commit]]
  //   // 数据通路
  //   io.commit.rmt.write(i).addr      := robRaw(i).logicReg
  //   io.commit.rmt.write(i).data      := robRaw(i).physicReg
  //   io.commit.freelist.free(i).bits  := robRaw(i).originReg
  //   io.commit.freelist.alloc(i).bits := robRaw(i).physicReg
  //   // 控制信号
  //   val wbValid = finalMask(i) && !hasException(i) && robRaw(i).regWEn
  //   io.commit.rmt.write(i).en         := wbValid
  //   io.commit.freelist.free(i).valid  := wbValid
  //   io.commit.freelist.alloc(i).valid := wbValid

  //   // TODO may be only work for ALU INSTR
  //   io.rob(i).ready := robRaw(i).regWEn && completeMask(i)
  // }
  // io.commit.chToArch := finalMask(0) && hasException(0)
  // io.recover         := finalMask(0) && hasException(0)

  // val branchCommit = WireInit(0.U.asTypeOf(new BranchCommit))

  // for (i <- 0 until nCommit) {
  //   when(finalMask(i) && robRaw(i).branch) {
  //     // commit to branch predictor
  //     branchCommit.valid  := 1.B
  //     branchCommit.pc     := robRaw(i).pc
  //     branchCommit.taken  := robRaw(i).branchTaken
  //     branchCommit.target := robRaw(i).computeBT
  //   }

  //   when(finalMask(i) && isStore(i)) {
  //     // commit to LSU & DCache
  //   }
  // }

  // io.branch := branchCommit
  // io.store  := DontCare

}

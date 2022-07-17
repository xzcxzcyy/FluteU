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

  val validMask = WireInit(VecInit(io.rob.map(r => r.valid)))

  val isStore = WireInit(VecInit(robRaw.map(r => r.memWMode =/= StoreMode.disable)))

  val completeMask = Wire(Vec(nCommit, Bool()))

  for (i <- 0 until nCommit) {
    var complete = robRaw(i).complete && (!isStore(i) || io.dCache.ready)
    for (j <- 0 until i) {
      complete = complete && robRaw(j).complete && (!isStore(i) || io.dCache.ready)
    }
    completeMask(i) := complete
  }

  val existMask = WireInit(VecInit((0 to 1).map(i => validMask(i) && completeMask(i))))

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
    programException(i) := existMask(i) && robRaw(i).exception.asUInt.orR
    branchFail(i)       := existMask(i) && robRaw(i).branch && robRaw(i).predictBT =/= robRaw(i).computeBT
  }
  
  val restMask = WireInit(VecInit(Seq.fill(nCommit)(1.B)))
  when (branchFail(0) && !existMask(1)) {
    restMask(0) := 0.B
  }
  when (programException(1)) {
    restMask(1) := 0.B
  }
  when (branchFail(1)) {
    restMask(1) := 0.B
  }

  val finalMask = WireInit(VecInit(
    for (i <- 0 until nCommit) yield {
      existMask(i) && storeMask(i) && cp0Mask(i) && restMask(i)
    }
  ))

  
  // [[io.commit]] 数据通路
  for (i <- 0 until nCommit) {
    io.commit.rmt.write(i).addr      := robRaw(i).logicReg
    io.commit.rmt.write(i).data      := robRaw(i).physicReg
    io.commit.freelist.free(i).bits  := robRaw(i).originReg
    io.commit.freelist.alloc(i).bits := robRaw(i).physicReg
    val wbValid = finalMask(i) && robRaw(i).regWEn
    io.commit.rmt.write(i).en         := wbValid
    io.commit.freelist.free(i).valid  := wbValid
    io.commit.freelist.alloc(i).valid := wbValid
  }

  val branchRecovery = branchFail(0) && finalMask(1)

  io.commit.chToArch := branchRecovery || io.intrReq
  io.recover := branchRecovery

  val branchTrain = WireInit(0.U.asTypeOf(Valid(new BranchTrain)))
  for (i <- 0 until nCommit) yield {
    // TODO: 分支训练时机可以选择
    when(branchFail(i)) {
      branchTrain.valid := 1.B
      branchTrain.bits.pc := robRaw(i).pc
      branchTrain.bits.taken := robRaw(i).branchTaken
      branchTrain.bits.target := robRaw(i).computeBT
    }
  }
  io.branch.train := branchTrain

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

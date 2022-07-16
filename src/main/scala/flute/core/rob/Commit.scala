package flute.core.rob

import chisel3._
import chisel3.util._
import flute.core.decode.StoreMode
import flute.core.rename.RenameCommit
import flute.config.CPUConfig._

class StoreCommit extends Bundle {}
class BranchCommit extends Bundle {
  val valid = Bool() // 全局有效位
  val pc = UInt(addrWidth.W) // 指令pc
  val taken = Bool() // 是否跳转
  val target = UInt(instrWidth.W)
}
class Commit(nCommit: Int) extends Module {
  val io = IO(new Bundle {
    val rob = Vec(nCommit, Flipped(Decoupled(new ROBEntry)))

    val commit = Flipped(new RenameCommit(nCommit))

    val store  = Output(new StoreCommit)
    val branch = Output(new BranchCommit)

    val recover = Output(Bool())
  })

  val robRaw = io.rob.map(r => r.bits)

  val validMask = WireDefault(VecInit(io.rob.map(r => r.valid)))

  val completeMask = Wire(Vec(nCommit, Bool()))
  for (i <- 0 until nCommit) {
    var complete = robRaw(i).complete
    for (j <- 0 until i) {
      complete = complete && robRaw(j).complete
    }
    completeMask(i) := complete
  }

  val branchMask = Wire(Vec(nCommit, Bool()))
  for (i <- 0 until nCommit) {
    var hasNoBranchBefore = 1.B
    for (j <- 0 until i) {
      hasNoBranchBefore = hasNoBranchBefore && !robRaw(i).branch
    }
    branchMask(i) := !robRaw(i).branch || hasNoBranchBefore
  }

  val storeMask = Wire(Vec(nCommit, Bool()))
  val isStore   = WireInit(VecInit(robRaw.map(r => r.memWMode =/= StoreMode.disable)))

  for (i <- 0 until nCommit) {
    var hasNoStoreBefore = 1.B
    for (j <- 0 until i) {
      hasNoStoreBefore = hasNoStoreBefore && !isStore(j)
    }
    storeMask(i) := !isStore(i) || hasNoStoreBefore
  }

  val programException = Wire(Vec(nCommit, Bool()))
  val branchException  = Wire(Vec(nCommit, Bool()))

  for (i <- 0 until nCommit) {
    programException(i) := robRaw(i).exception.asUInt.orR
    branchException(i)  := robRaw(i).branch && robRaw(i).predictBT =/= robRaw(i).computeBT
  }
  val hasException = (0 until nCommit).map(i => programException(i) || branchException(i))

  val exceptionMask = Wire(Vec(nCommit, Bool()))
  exceptionMask(0) := 1.B
  for (i <- 1 until nCommit) {
    var hasNoExcptBefore = 1.B
    for (j <- 0 to i) { // include itself
      hasNoExcptBefore = hasNoExcptBefore && !hasException(j)
    }
    exceptionMask(i) := hasNoExcptBefore
  }

  val finalMask = WireDefault(
    VecInit(
      (0 until nCommit).map(i =>
        validMask(i) && completeMask(i) && branchMask(i) && storeMask(i) && exceptionMask(i)
      )
    )
  )

  // Rename Commit
  for (i <- 0 until nCommit) {
    // [[io.commit]]
    // 数据通路
    io.commit.rmt.write(i).addr      := robRaw(i).logicReg
    io.commit.rmt.write(i).data      := robRaw(i).physicReg
    io.commit.freelist.free(i).bits  := robRaw(i).originReg
    io.commit.freelist.alloc(i).bits := robRaw(i).physicReg
    // 控制信号
    val wbValid = finalMask(i) && !hasException(i) && robRaw(i).regWEn
    io.commit.rmt.write(i).en         := wbValid
    io.commit.freelist.free(i).valid  := wbValid
    io.commit.freelist.alloc(i).valid := wbValid

    // TODO may be only work for ALU INSTR
    io.rob(i).ready := robRaw(i).regWEn && completeMask(i)
  }
  io.commit.chToArch := finalMask(0) && hasException(0)
  io.recover         := finalMask(0) && hasException(0)

  for (i <- 0 until nCommit) {
    when(finalMask(i) && robRaw(i).branch) {
      // commit to branch predictor
    }

    when(finalMask(i) && isStore(i)) {
      // commit to LSU & DCache
    }
  }

  io.branch := DontCare
  io.store  := DontCare

}

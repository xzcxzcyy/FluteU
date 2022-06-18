package flute.core.rob

import chisel3._
import chisel3.util._
import flute.core.decode.StoreMode
import flute.core.rename.RenameCommit

class Commit(nCommit: Int) extends Module {
  val io = IO(new Bundle {
    val rob = Vec(nCommit, Flipped(Decoupled(new ROBEntry)))

    val commit = Flipped(new RenameCommit(nCommit))
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
  val isStore   = Wire(VecInit(robRaw.map(r => r.memWMode =/= StoreMode.disable)))

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
  for (i <- 0 until nCommit) {
    var hasNoExcptBefore = 1.B
    for (j <- 0 until i) {
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

  for (i <- 0 until nCommit) {
    // [[io.commit]]
    // 数据通路
    io.commit.rmt.write(i).addr := robRaw(i).logicReg
    io.commit.rmt.write(i).data := robRaw(i).physicReg

    io.commit.freelist.free(i).bits  := robRaw(i).originReg
    io.commit.freelist.alloc(i).bits := robRaw(i).physicReg

    // 控制信号
    val wbValid = finalMask(i) && !hasException(i) && robRaw(i).regWEn

    io.commit.rmt.write(i).en := wbValid

    io.commit.freelist.free(i).valid  := wbValid
    io.commit.freelist.alloc(i).valid := wbValid

    io.commit.chToArch := finalMask(i) && hasException(i)
  }

}

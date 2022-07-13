package flute.core

import chisel3._
import chisel3.util._
import flute.core.fetch._
import flute.core.decode._
import flute.core.rename._
import flute.core.components.StageReg
import flute.util.ValidBundle
import flute.core.issue.Dispatch
import flute.core.rob.ROB

class Backend(nWays: Int = 2) extends Module {
  require(nWays == 2)
  val io = IO(new Bundle {
    val ibuffer = Vec(nWays, Flipped(DecoupledIO(new IBEntry)))
  })

  val decoders  = for (i <- 0 until nWays) yield Module(new Decoder)
  val rename    = Module(new Rename(nWays = nWays, nCommit = nWays))
  val dispatch  = Module(new Dispatch)
  val rob       = Module(new ROB(numEntries = 128, numRead = 2, numWrite = 2, numSetComplete = 4))
  val busyTable = Module(new BusyTable(nRead = 6, nCheckIn = 2, nCheckOut = 4))

  val decodeStage = Module(new StageReg(Vec(nWays, Valid(new MicroOp))))
  val renameStage = Module(new StageReg(Vec(nWays, Valid(new MicroOp(rename = true)))))

  for (i <- 0 until nWays) {
    decoders(i).io.instr       := io.ibuffer(i).bits
    decodeStage.io.in(i).bits  := decoders(i).io.microOp
    decodeStage.io.in(i).valid := io.ibuffer(i).valid
  }
  rename.io.decode     := decodeStage.io.data
  busyTable.io.checkIn := rename.io.checkIn
  rob.io.write <> rename.io.rob
  renameStage.io.in := rename.io.dispatch
  dispatch.io.in    := renameStage.io.data

  // stall
  rename.io.stall := dispatch.io.stallReq
  val stall = rename.io.stallReq || dispatch.io.stallReq
  for (i <- 0 until nWays) {
    io.ibuffer(i).ready := !stall
  }
  decodeStage.io.valid := !stall
  renameStage.io.valid := !(dispatch.io.stallReq)
}

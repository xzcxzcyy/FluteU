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
import flute.core.issue.AluIssueQueue
import flute.core.issue.AluPipeline
import flute.core.issue.AluIssue
import flute.core.issue.AluEntry
import flute.core.components.RegFile
import flute.config.CPUConfig._
import flute.core.rob.Commit
import flute.core.issue.LsuIssue
import flute.core.execute.LsuPipeline

class Backend(nWays: Int = 2) extends Module {
  require(nWays == 2)
  val io = IO(new Bundle {
    val ibuffer   = Vec(nWays, Flipped(DecoupledIO(new IBEntry)))
    val prf       = Output(Vec(phyRegAmount, UInt(dataWidth.W)))
    val rmt       = new RMTDebugOut
    val busyTable = Output(Vec(phyRegAmount, Bool()))
  })

  val decoders  = for (i <- 0 until nWays) yield Module(new Decoder)
  val rename    = Module(new Rename(nWays = nWays, nCommit = nWays))
  val dispatch  = Module(new Dispatch)
  val rob       = Module(new ROB(numEntries = 128, numRead = 2, numWrite = 2, numSetComplete = 2))
  val busyTable = Module(new BusyTable(nRead = 10, nCheckIn = 2, nCheckOut = 3))
  val commit    = Module(new Commit(nCommit = nWays))

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

  commit.io.rob <> rob.io.read
  rename.io.commit := commit.io.commit

  private val detectWidth = 4
  private val nAluPl      = 2 // number of alu piplines

  val aluIssueQueue = Module(new AluIssueQueue(30, detectWidth))
  val aluIssue      = Module(new AluIssue(detectWidth))
  val aluPipeline   = for (i <- 0 until nAluPl) yield Module(new AluPipeline)

  val lsuIssueQueue = Module(new Queue(new MicroOp(rename = true), entries = 30, hasFlush = true))
  val lsuIssue      = Module(new LsuIssue)
  val lsuPipeline   = Module(new LsuPipeline)

  val regfile = Module(new RegFile(numRead = 3, numWrite = 3))

  dispatch.io.out(0) <> aluIssueQueue.io.enq(0)
  dispatch.io.out(1) <> aluIssueQueue.io.enq(1)
  dispatch.io.out(2) <> lsuIssueQueue.io.enq
  dispatch.io.out(3).ready := 0.B

  //---------------- AluIssueQueue + AluPipelines ------------------ //

  aluIssue.io.detect     := aluIssueQueue.io.data
  aluIssueQueue.io.issue := aluIssue.io.issue

  for (i <- 0 until 2 * detectWidth) {
    aluIssue.io.bt(i) <> busyTable.io.read(i)
  }

  val aluIssueStage = Module(new StageReg(Vec(nAluPl, Valid(new AluEntry))))
  aluIssueStage.io.in := aluIssue.io.out

  aluIssue.io.wake := aluIssueStage.io.data

  val bypass = Wire(Vec(nAluPl, UInt(dataWidth.W)))

  // aluPipeline 默认注册在其他组件(regfile, busyTable)接口的低位

  for (i <- 0 until nAluPl) {
    aluPipeline(i).io.uop := aluIssueStage.io.data(i)
    bypass(i)             := aluPipeline(i).io.bypass.out

    aluPipeline(i).io.prf <> regfile.io.read(i)
    aluPipeline(i).io.bypass.in := bypass
    regfile.io.write(i)         := aluPipeline(i).io.wb.prf
    busyTable.io.checkOut(i)    := aluPipeline(i).io.wb.busyTable
    rob.io.setComplete(i)       := aluPipeline(i).io.wb.rob
  }

  /// other interface
  decodeStage.io.flush   := 0.B
  renameStage.io.flush   := 0.B
  aluIssueStage.io.flush := 0.B
  aluIssueQueue.io.flush := 0.B

  aluIssueStage.io.valid := 1.B

  // ---------------- LSU ------------------ //
  lsuIssue.io.in <> lsuIssueQueue.io.deq
  for (i <- 0 to 1) {
    lsuIssue.io.bt(i) <> busyTable.io.read(2 * detectWidth + i)
  }
  lsuPipeline.io.uop <> lsuIssue.io.out
  // lsuPipeline.io.dcache // TODO: DCache Ports.
  lsuPipeline.io.flush := 0.B // TODO: flush
  lsuPipeline.io.prf <> regfile.io.read(2)

  regfile.io.write(2)      := lsuPipeline.io.wb.prf
  busyTable.io.checkOut(2) := lsuPipeline.io.wb.busyTable
  rob.io.setComplete(2)    := lsuPipeline.io.wb.rob

  // debug
  io.prf       := regfile.io.debug
  io.busyTable := VecInit(busyTable.io.debug.table.asBools)
  io.rmt       := rename.io.rmtDebug

}

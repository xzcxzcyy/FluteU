package flute.core.backend

import chisel3._
import chisel3.util._
import flute.core.frontend._
import flute.core.backend.decode._
import flute.core.backend.rename._
import flute.core.components.StageReg
import flute.util.ValidBundle
import flute.core.backend.dispatch.Dispatch
import flute.core.backend.commit.ROB
import flute.core.backend.alu.AluIssueQueue
import flute.core.backend.alu.AluPipeline
import flute.core.backend.alu.AluIssue
import flute.core.backend.alu.AluEntry
import flute.core.components.RegFile
import flute.config.CPUConfig._
import flute.core.backend.commit.Commit
import flute.core.backend.lsu.LsuIssue
import flute.core.backend.lsu.LsuPipeline
import flute.cache.top.DCacheWithCore
import flute.core.backend.commit.BranchCommit
import flute.cp0.CP0WithCommit
import flute.core.backend.commit.ROBEntry

class TraceBundle extends Bundle {
  val pc       = UInt(32.W)
  val arfWEn   = Bool()
  val arfWAddr = UInt(5.W)
  val arfWData = UInt(32.W)
}

class Backend(nWays: Int = 2) extends Module {
  require(nWays == 2)
  val io = IO(new Bundle {
    val ibuffer = Vec(nWays, Flipped(DecoupledIO(new IBEntry)))
    // Debug out //
    val prf       = Output(Vec(phyRegAmount, UInt(dataWidth.W)))
    val rmt       = new RMTDebugOut
    val busyTable = Output(Vec(phyRegAmount, Bool()))
    val arfWTrace = Output(new TraceBundle)
    // ========= //
    val dcache       = Flipped(new DCacheWithCore)
    val branchCommit = Output(new BranchCommit)
    val cp0          = Flipped(new CP0WithCommit)
    val cp0IntrReq   = Input(Bool())
  })

  val decoders = for (i <- 0 until nWays) yield Module(new Decoder)
  val dispatch = Module(new Dispatch)
  val rob = Module(
    new ROB(numEntries = robEntryAmount, numRead = 2, numWrite = 2, numSetComplete = 3)
  )
  val regfile   = Module(new RegFile(numRead = 3, numWrite = 3))
  val rename    = Module(new Rename(nWays = nWays, nCommit = nWays))
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
  rename.io.commit          := commit.io.commit
  io.branchCommit           := commit.io.branch
  io.cp0                    := commit.io.cp0
  commit.io.intrReq         := io.cp0IntrReq
  commit.io.store.req.ready := io.dcache.req.ready
  val dCacheReqWire  = WireInit(commit.io.store.req.bits)
  val dCacheReqValid = WireInit(commit.io.store.req.valid)
  // 对 [[io.dcache.req.bits]] 的赋值，将在后面进行

  private val detectWidth = 4
  private val nAluPl      = 2 // number of alu piplines

  val aluIssueQueue = Module(new AluIssueQueue(30, detectWidth))
  val aluIssue      = Module(new AluIssue(detectWidth))
  val aluPipeline   = for (i <- 0 until nAluPl) yield Module(new AluPipeline)

  val lsuIssueQueue = Module(new Queue(new MicroOp(rename = true), 32, hasFlush = true))
  val lsuIssue      = Module(new LsuIssue)
  val lsuPipeline   = Module(new LsuPipeline)

  val needFlush = io.cp0IntrReq || commit.io.branch.pcRestore.valid || commit.io.cp0.eret

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
  decodeStage.io.flush   := needFlush
  renameStage.io.flush   := needFlush
  aluIssueStage.io.flush := needFlush
  aluIssueQueue.io.flush := needFlush
  for (i <- 0 to 1) yield {
    aluPipeline(i).io.flush := needFlush
  }

  aluIssueStage.io.valid := 1.B

  // ---------------- LSU ------------------ //
  lsuIssue.io.in <> lsuIssueQueue.io.deq
  lsuIssue.io.flush          := needFlush
  lsuIssueQueue.io.flush.get := needFlush
  for (i <- 0 to 1) {
    lsuIssue.io.bt(i) <> busyTable.io.read(2 * detectWidth + i)
  }
  lsuPipeline.io.uop <> lsuIssue.io.out
  lsuPipeline.io.sbRetire         := commit.io.sbRetire
  lsuPipeline.io.dcache.hazard    := commit.io.store.hazard
  lsuPipeline.io.dcache.req.ready := io.dcache.req.ready
  when(!commit.io.store.hazard) {
    dCacheReqValid := lsuPipeline.io.dcache.req.valid
    dCacheReqWire  := lsuPipeline.io.dcache.req.bits
  }
  lsuPipeline.io.dcache.resp := io.dcache.resp
  lsuPipeline.io.flush       := needFlush
  lsuPipeline.io.prf <> regfile.io.read(2)
  regfile.io.write(2)      := lsuPipeline.io.wb.prf
  busyTable.io.checkOut(2) := lsuPipeline.io.wb.busyTable
  rob.io.setComplete(2)    := lsuPipeline.io.wb.rob

  // ---------------- Data Cache ------------------ //
  io.dcache.req.valid := dCacheReqValid
  io.dcache.req.bits  := dCacheReqWire

  // debug
  io.prf       := regfile.io.debug
  io.busyTable := VecInit(busyTable.io.debug.table.asBools)
  io.rmt       := rename.io.rmtDebug

  rob.io.flush      := needFlush
  dispatch.io.flush := needFlush
  io.dcache.flush   := needFlush

  // debug traceBuffer
  val traceBuffer = Module(new Ibuffer(new ROBEntry, 128, 1, 2))
  for (i <- 0 to 1) yield {
    traceBuffer.io.write(i).valid := rob.io.read(i).fire
    traceBuffer.io.write(i).bits  := rob.io.read(i).bits
  }
  traceBuffer.io.read(0).ready := 1.B
  val traceBRead = traceBuffer.io.read(0)
  io.arfWTrace.arfWEn   := traceBRead.fire && traceBRead.bits.regWEn
  io.arfWTrace.arfWAddr := traceBRead.bits.logicReg
  io.arfWTrace.arfWData := traceBRead.bits.regWData
  io.arfWTrace.pc       := traceBRead.bits.pc

  traceBuffer.io.flush := 0.B
}

package core

import chisel3._
import config.CpuConfig._
import core.pipeline._
import core.pipeline.stagereg._

class CPUTop(iMemFile:String = "test_data/imem.in", dMemFile:String = "test_data/dmem.in") extends Module {
  val io = IO(new Bundle {
    val regfileDebug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })

  val ifid  = Module(new IfIdStage)
  val idex  = Module(new IdExStage)
  val exmem = Module(new ExMemStage)
  val memwb = Module(new MemWbStage)

  val fetch     = Module(new Fetch(iMemFile))
  val decode    = Module(new Decode)
  val execution = Module(new Execution)
  val memAcc    = Module(new MemoryAccess(dMemFile))
  val writeBack = Module(new WriteBack)

  io.regfileDebug := decode.io.regfileDebug

  ifid.io.in           := fetch.io.toDecode
  decode.io.fromIf     := ifid.io.data
  idex.io.in           := decode.io.toEx
  execution.io.fromId  := idex.io.data
  exmem.io.in          := execution.io.toMem
  memAcc.io.fromEx     := exmem.io.data
  memwb.io.in          := memAcc.io.toWb
  writeBack.io.fromMem := memwb.io.data

  // memory stall requests
  val memStallReq = memAcc.io.dMemStallReq | fetch.io.iMemStallReq

  decode.io.writeBackAddr := writeBack.io.writeRegAddr
  decode.io.writeBackData := writeBack.io.writeBackData
  decode.io.writeBackEn   := writeBack.io.regWriteEn & ~memStallReq

  fetch.io.branchAddr  := decode.io.branchAddr
  fetch.io.branchTaken := decode.io.branchTaken

  ifid.io.valid    := ~memStallReq
  idex.io.valid    := ~memStallReq
  exmem.io.valid   := ~memStallReq
  memwb.io.valid   := ~memStallReq
  fetch.io.pcStall := memStallReq

  ifid.io.flush  := 0.B
  idex.io.flush  := 0.B
  exmem.io.flush := 0.B
  memwb.io.flush := 0.B
}

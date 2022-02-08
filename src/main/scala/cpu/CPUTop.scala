package cpu

import chisel3._
import pipeline.IfIdStage
import pipeline.IdExStage
import pipeline.ExMemStage
import pipeline.MemWbStage
import pipeline.Fetch
import pipeline.Decode
import pipeline.Execution
import pipeline.MemoryAccess
import pipeline.WriteBack

class CPUTop extends Module {
  val io = IO(new Bundle {})

  val ifid  = Module(new IfIdStage)
  val idex  = Module(new IdExStage)
  val exmem = Module(new ExMemStage)
  val memwb = Module(new MemWbStage)

  val fetch     = Module(new Fetch)
  val decode    = Module(new Decode)
  val execution = Module(new Execution)
  val memAcc    = Module(new MemoryAccess)
  val writeBack = Module(new WriteBack)

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

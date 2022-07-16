package flute.core.execute

import chisel3._
import chisel3.util._
import flute.core.components.RegFileReadIO
import flute.core.issue.AluWB
import flute.core.decode.MicroOp
import flute.core.decode.StoreMode
import flute.core.rob.ROBCompleteBundle
import flute.config.CPUConfig._
import flute.cp0.ExceptionBundle
import flute.core.decode.LoadMode

class LsuPipeline extends Module {
  val io = IO(new Bundle {
    val uop    = Flipped(DecoupledIO(new MicroOp))
    val prf    = Flipped(new RegFileReadIO)
    val wb     = Output(new AluWB)
    val dcache = new LSUWithDCacheIO
    val flush  = Input(Bool())
  })

  val readIn = io.uop
  io.prf.r1Addr := readIn.bits.rsAddr
  io.prf.r2Addr := readIn.bits.rtAddr
  val (op1, op2) = (io.prf.r1Data, io.prf.r2Data)
  val read2Ex    = WireInit(readIn.bits)
  read2Ex.op1.op := op1
  when(readIn.bits.storeMode =/= StoreMode.disable) {
    read2Ex.op2.op := op2
  }

  val lsu = Module(new LSU)
  lsu.io.dcache <> io.dcache
  lsu.io.flush := io.flush

  io.uop.ready       := lsu.io.instr.ready
  lsu.io.instr.valid := io.uop.valid
  lsu.io.instr.bits  := read2Ex

  val writeRob  = WireInit(0.U.asTypeOf(new ROBCompleteBundle(robEntryNumWidth)))
  val lsuMemReq = lsu.io.toRob.bits

  val prfWriteEnable = lsu.io.toRob.valid && lsuMemReq.loadMode =/= LoadMode.disable

  // wb.rob
  writeRob.valid     := lsu.io.toRob.valid
  writeRob.robAddr   := lsuMemReq.robAddr
  writeRob.exception := 0.U.asTypeOf(new ExceptionBundle) // TODO: Exception
  writeRob.regWEn    := (lsuMemReq.loadMode =/= LoadMode.disable)
  writeRob.regWData  := lsuMemReq.data
  writeRob.memWMode  := lsuMemReq.storeMode
  writeRob.memWAddr  := lsuMemReq.addr
  writeRob.memWData  := lsuMemReq.data
  io.wb.rob          := writeRob

  // wb.busyTable
  val btCheckOut = WireInit(0.U.asTypeOf(Valid(UInt(phyRegAddrWidth.W))))
  btCheckOut.valid := prfWriteEnable
  btCheckOut.bits  := lsuMemReq.writeRegAddr
  io.wb.busyTable  := btCheckOut

  // wb.prf
  io.wb.prf.writeEnable := prfWriteEnable
  io.wb.prf.writeAddr   := lsuMemReq.writeRegAddr
  io.wb.prf.writeData   := lsuMemReq.data
}

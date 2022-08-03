package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.core.backend.alu.AluWB
import flute.core.backend.rename.BusyTableReadPort
import flute.core.components.RegFileReadIO
import flute.cp0.CP0Read

class MDUTop extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new MicroOp(rename = true)))
    val wb = Output(new AluWB)

    val bt   = Vec(2, Flipped(new BusyTableReadPort))
    val prf  = Flipped(new RegFileReadIO)
    val hilo = Input(new HILORead)
    val cp0  = Flipped(new CP0Read)

    val retire = Input(Bool())
    val flush  = Input(Bool())
  })

  val idle :: busy :: Nil = Enum(2)
  val state               = RegInit(idle)

  val mduIssue  = Module(new MduIssue)
  val mduRead   = Module(new MduRead)
  val mduExcute = Module(new MduExcute)

  io.in.ready          := (state === idle) && mduIssue.io.in.ready
  mduIssue.io.in.valid := (state === idle) && io.in.valid
  mduIssue.io.in.bits  := io.in.bits
  mduRead.io.in <> mduIssue.io.out
  mduExcute.io.in <> mduRead.io.out
  io.wb := mduExcute.io.wb

  mduIssue.io.bt <> io.bt
  mduRead.io.prf <> io.prf
  mduExcute.io.cp0 <> io.cp0
  mduExcute.io.hilo := io.hilo

  // flush
  mduIssue.io.flush  := io.flush
  mduRead.io.flush   := io.flush
  mduExcute.io.flush := io.flush

  switch(state) {
    is(idle) {
      when(io.in.valid) {
        state := busy
      }
    }
    is(busy) {
      when(io.flush || io.retire) {
        state := idle
      }
    }
  }

}

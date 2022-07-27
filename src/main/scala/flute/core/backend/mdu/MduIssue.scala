package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.core.backend.rename.BusyTableReadPort

class MduIssue extends Module {
    val io = IO(new Bundle {
        val in = Flipped(Decoupled(new MicroOp(rename = true)))

        val bt = Vec(2, Flipped(new BusyTableReadPort))

        val out = Decoupled(new MicroOp(rename = true))
    })

    // stage 2: Issue
    val uop = WireInit(io.in.bits)
    val available = Wire(Bool())
    val bt = Wire(Vec(2, Bool()))
}

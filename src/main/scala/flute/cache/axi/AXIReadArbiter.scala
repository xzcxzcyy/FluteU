package flute.cache.axi

import chisel3._
import chisel3.util._
import flute.axi.AXIIO

class AXIReadArbiter(masterCount: Int) extends Module {
  val io = IO(new Bundle {
    val masters = Vec(masterCount, AXIIO.master())
    val bus     = AXIIO.master()
  })

  val idle :: lock :: Nil = Enum(2)

  val state = RegInit(idle)
  val arSel = RegInit(0.U(log2Ceil(masterCount).W))

  val arValidMask = VecInit(io.masters.map(_.ar.valid))

  io.bus.aw := DontCare
  io.bus.w  := DontCare
  io.bus.b  := DontCare
  for (i <- 0 until masterCount) {
    when(i.U === arSel) {
      io.masters(i).ar <> io.bus.ar
      io.masters(i).r <> io.bus.r
    }.otherwise {
      io.masters(i).ar.ready := 0.B
      io.masters(i).r.valid  := 0.B
      io.masters(i).r.bits   := DontCare
    }
  }

  switch(state) {
    is(idle) {
      arSel := PriorityEncoder(arValidMask)
      state := lock
    }

    is(lock) {
      when(io.bus.r.bits.last && io.bus.r.fire) {
        state := idle
      }
    }
  }

}

package flute.cache.axi

import chisel3._
import chisel3.util._
import flute.axi.AXIIO

class AXIWriteArbiter(masterCount: Int) extends Module {
  val io = IO(new Bundle {
    val masters = Vec(masterCount, AXIIO.master())
    val bus     = AXIIO.master()
  })

  val idle :: lock :: Nil = Enum(2)

  val state = RegInit(idle)
  val awSel = RegInit(0.U(log2Ceil(masterCount).W))

  val awValidMask = VecInit(io.masters.map(_.aw.valid))

  io.bus.ar := DontCare
  io.bus.r  := DontCare
  for (i <- 0 until masterCount) {
    when(i.U === awSel) {
      io.masters(i).aw <> io.bus.aw
      io.masters(i).w <> io.bus.w
      io.masters(i).b <> io.bus.b
    }.otherwise {
      io.masters(i).aw.ready := 0.B
      io.masters(i).w.ready  := 0.B
      io.masters(i).b.valid  := 0.B
      io.masters(i).b.bits   := DontCare
    }
  }

  switch(state) {
    is(idle) {
      awSel := PriorityEncoder(awValidMask)
      state := lock
    }

    is(lock) {
      when(io.bus.w.bits.last && io.bus.w.fire) {
        state := idle
      }
    }
  }

}

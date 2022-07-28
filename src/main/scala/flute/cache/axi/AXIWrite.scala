package flute.cache.axi

import chisel3._
import chisel3.util._
import flute.axi.AXIIO
import flute.config.CPUConfig._
import flute.config.CacheConfig
import flute.core.backend.decode.StoreMode

class AXIWirte(axiId: UInt) extends Module {
  val io = IO(new Bundle {
    val req = Flipped(DecoupledIO(new Bundle {
      val addr      = UInt(addrWidth.W) // 按字节对齐
      val storeMode = UInt(StoreMode.width.W)
      val data      = UInt(dataWidth.W)
    }))

    val resp = Output(Bool()) // 写数据传输完成后一拍返回true

    val axi = AXIIO.master()
  })

  val dataBuffer = RegInit(0.U(dataWidth.W))
  val addrBuffer = RegInit(0.U(addrWidth.W))
  val strbBuffer = RegInit(0.U(4.W))

  val idle :: active :: transfer :: finish :: Nil = Enum(4)

  val state = RegInit(idle)

  // axi config
  io.axi.ar := DontCare
  io.axi.r  := DontCare

  io.axi.aw.bits.addr  := addrBuffer
  io.axi.aw.bits.id    := axiId
  io.axi.aw.bits.burst := "b01".U(2.W)
  io.axi.aw.bits.len   := 0.U(4.W)       /// 只传输1拍
  io.axi.aw.bits.size  := "b010".U(3.W)
  io.axi.aw.bits.cache := 0.U
  io.axi.aw.bits.prot  := 0.U
  io.axi.aw.bits.lock  := 0.U

  io.axi.w.bits.data := dataBuffer
  io.axi.w.bits.id   := axiId
  io.axi.w.bits.last := 1.B
  io.axi.w.bits.strb := strbBuffer

  // always ignore b channel response ( no cache error )
  io.axi.b.ready := 1.B

  switch(state) {
    is(idle) {
      when(io.req.fire) {
        addrBuffer := io.req.bits.addr
        dataBuffer := io.req.bits.data
        strbBuffer := MuxLookup(
          io.req.bits.storeMode,
          "b1111".U,
          Seq(
            StoreMode.word     -> "b1111".U,
            StoreMode.halfword -> "b0011".U,
            StoreMode.byte     -> "b0001".U
          )
        )

        state := active
      }
    }

    is(active) {
      when(io.axi.aw.fire) {
        state := transfer
      }
    }

    is(transfer) {
      when(io.axi.w.fire) {
        state := finish
      }
    }

    is(finish) {
      state := idle
    }
  }
}

package flute.cache.axi

import chisel3._
import chisel3.util._
import flute.axi.AXIIO
import flute.config.CPUConfig._
import flute.config.CacheConfig

/**
  * AXIRead FSM who buffers both the req and resp
  *
  * @param axiId : AXI ID
  * @param len   : 传输长度，单位：32bit
  */
class AXIBankRead(axiId: Int)(implicit cacheConfig: CacheConfig) extends Module {
  private val len = (cacheConfig.numOfBanks - 1)

  val io = IO(new Bundle {
    val req  = Flipped(DecoupledIO(UInt(addrWidth.W)))
    val resp = ValidIO(UInt((dataWidth * len).W))

    val axi = AXIIO.master()
  })

  val addrBuffer = RegInit(0.U(addrWidth.W))
  val dataBuffer = RegInit(VecInit(Seq.fill(len)(0.U(dataWidth.W))))
  val index      = RegInit(0.U(cacheConfig.bankIndexLen.W))

  val idle :: active :: transfer :: finish :: Nil = Enum(4)

  val state = RegInit(idle)

  // axi config
  io.axi.aw := DontCare
  io.axi.w  := DontCare
  io.axi.b  := DontCare

  io.axi.ar.bits.id    := axiId.U(4.W)
  io.axi.ar.bits.addr  := Mux(state === active, addrBuffer, io.req.bits)
  io.axi.ar.bits.len   := len.U(4.W)
  io.axi.ar.bits.size  := "b010".U(3.W) // always 4 bytes
  io.axi.ar.bits.burst := "b10".U(2.W)  // axi wrap burst
  io.axi.ar.bits.lock  := 0.U
  io.axi.ar.bits.cache := 0.U
  io.axi.ar.bits.prot  := 0.U

  switch(state) {
    is(idle) {
      when(io.req.fire) {
        addrBuffer := io.req.bits
        index      := cacheConfig.getBankIndex(io.req.bits)

        state := active
      }
    }

    is(active) {
      when(io.axi.ar.fire) {
        state := transfer
      }
    }

    is(transfer) {
      when(io.axi.r.fire && io.axi.r.bits.id === axiId.U) {
        dataBuffer(index) := io.axi.r.bits
        index             := index + 1.U

        when(io.axi.r.bits.last) {
          state := finish
        }
      }
    }

    is(finish) {
      state := idle
    }
  }

  io.axi.ar.valid := (state === active)
  io.axi.r.ready  := (state === transfer)

  io.req.ready  := (state === idle)
  io.resp.valid := (state === finish)

}

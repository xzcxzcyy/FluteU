package flute.cache.axi

import chisel3._
import chisel3.util._
import flute.axi.AXIIO
import flute.config.CPUConfig._
import flute.config.CacheConfig

class AXIBankWirte(axiId: UInt)(implicit cacheConfig: CacheConfig) extends Module {
  import cacheConfig.{getTag, getIndex, bankIndexLen, bankOffsetLen}

  private val len = (cacheConfig.numOfBanks - 1)
  val io = IO(new Bundle {
    val req = Flipped(DecoupledIO(new Bundle {
      val addr = UInt(addrWidth.W)
      val data = UInt((dataWidth * len).W)
    }))

    val resp = Output(Bool()) // 写数据传输完成后一拍返回true

    val axi = AXIIO.master()
  })

  val dataBuffer = RegInit(VecInit(Seq.fill(len)(0.U(dataWidth.W))))
  val addrBuffer = RegInit(0.U(addrWidth.W))
  val index      = RegInit(0.U(bankIndexLen.W))

  val idle :: active :: transfer :: finish :: Nil = Enum(4)

  val state = RegInit(idle)

  val last = WireInit(index === len.U)

  // axi config
  io.axi.ar := DontCare
  io.axi.r  := DontCare

  io.axi.aw.bits.addr  := addrBuffer
  io.axi.aw.bits.id    := axiId
  io.axi.aw.bits.burst := "b01".U(2.W)
  io.axi.aw.bits.len   := len.U(4.W)
  io.axi.aw.bits.size  := "b010".U(3.W)
  io.axi.aw.bits.cache := 0.U
  io.axi.aw.bits.prot  := 0.U
  io.axi.aw.bits.lock  := 0.U

  io.axi.w.bits.data := dataBuffer(index)
  io.axi.w.bits.id   := axiId
  io.axi.w.bits.last := last
  io.axi.w.bits.strb := "b1111".U(4.W)

  // always ignore b channel response ( no cache error )
  io.axi.b.ready := 1.B

  switch(state) {
    is(idle) {
      when(io.req.fire) {
        val addr = io.req.bits.addr
        addrBuffer := Cat(getTag(addr), getIndex(addr), 0.U(bankIndexLen + bankOffsetLen))
        dataBuffer := io.req.bits.data
        index      := 0.U

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
        index := index + 1.U
        when(last) {
          state := finish
        }
      }
    }

    is(finish) {
      state := idle
    }
  }

  io.axi.aw.valid := (state === active)
  io.axi.w.valid  := (state === transfer)

  io.req.ready := (state === idle)
  io.resp      := (state === finish)
}

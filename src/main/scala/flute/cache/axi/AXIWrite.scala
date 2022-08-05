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

  val maskBuffer    = RegInit(0.U(4.W))
  val retDataBuffer = RegInit(0.U(32.W))

  val idle :: active :: transfer :: finish :: Nil = Enum(4)

  val state = RegInit(idle)

  // axi config
  io.axi.ar := DontCare
  io.axi.r  := DontCare

  io.axi.aw.bits.addr  := addrBuffer
  io.axi.aw.bits.id    := axiId
  io.axi.aw.bits.burst := "b01".U(2.W)
  io.axi.aw.bits.len   := 0.U(4.W) /// 只传输1拍
  io.axi.aw.bits.size  := "b010".U(3.W)
  io.axi.aw.bits.cache := 0.U
  io.axi.aw.bits.prot  := 0.U
  io.axi.aw.bits.lock  := 0.U

  io.axi.w.bits.data := retDataBuffer
  io.axi.w.bits.id   := axiId
  io.axi.w.bits.last := 1.B
  io.axi.w.bits.strb := maskBuffer

  // always ignore b channel response ( no cache error )
  io.axi.b.ready := 1.B

  switch(state) {
    is(idle) {
      when(io.req.fire) {
        addrBuffer := io.req.bits.addr
        dataBuffer := io.req.bits.data
        val (mask, retData) =
          AXIUtil.storeConvert(io.req.bits.storeMode, io.req.bits.addr(1, 0), io.req.bits.data)
        maskBuffer    := mask
        retDataBuffer := retData

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

  io.axi.w.valid  := (state === transfer)
  io.axi.aw.valid := (state === active)
  io.req.ready    := (state === idle)
  io.resp         := (state === finish)
}

object AXIUtil {
  def storeConvert(storeMode: UInt, offset: UInt, data: UInt) = {
    assert(storeMode.getWidth == StoreMode.width && offset.getWidth == 2 && data.getWidth == 32)
    val mask    = Wire(UInt(4.W))
    val retData = Wire(UInt(32.W))

    val halfWordData = Mux(offset(1), Cat(data(15, 0), 0.U(16.W)), data)
    val byteData = MuxLookup(
      offset,
      data,
      Seq(
        "b00".U -> data,
        "b01".U -> Cat(0.U(16.W), data(7, 0), 0.U(8.W)),
        "b10".U -> Cat(0.U(8.W), data(7, 0), 0.U(16.W)),
        "b11".U -> Cat(data(7, 0), 0.U(24.W)),
      )
    )
    val halfWordMask = Mux(offset(1), "b1100".U, "b0011".U)
    val byteDataMask = MuxLookup(
      offset,
      "b0001".U,
      Seq(
        "b00".U -> "b0001".U,
        "b01".U -> "b0010".U,
        "b10".U -> "b0100".U,
        "b11".U -> "b1000".U,
      )
    )

    mask := MuxLookup(
      storeMode,
      "b1111".U,
      Seq(
        StoreMode.word     -> "b1111".U,
        StoreMode.halfword -> halfWordMask,
        StoreMode.byte     -> byteDataMask,
      )
    )

    retData := MuxLookup(
      storeMode,
      data,
      Seq(
        StoreMode.word     -> data,
        StoreMode.halfword -> halfWordData,
        StoreMode.byte     -> byteData,
      )
    )

    (mask, retData)
  }
}

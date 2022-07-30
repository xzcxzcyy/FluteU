package flute.cache

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

import flute.config.CPUConfig._
import flute.config.CacheConfig
import flute.util.BitMode.fromIntToBitModeLong
import flute.axi.AXIIO
import flute.cache.axi.AXIReadPort
import flute.cache.components.ReFillBuffer
import flute.cache.top.ICacheWithCore
import flute.cache.top.ICacheResp
import flute.cache.top.ICacheReq

class ICacheIO extends Bundle {
  val addr = Flipped(DecoupledIO(UInt(addrWidth.W)))
  val data = DecoupledIO(Vec(fetchGroupSize, UInt(dataWidth.W)))
}

/**
  * for now ICache is a default miss Cache wraped by axi interface
  *
  * @param cacheConfig as its name
  */

class ICacheWithAXI(cacheConfig: CacheConfig) extends Module {
  val io = IO(new Bundle {
    val request = new ICacheIO
    val axi     = AXIIO.master()
  })
  implicit val config = cacheConfig

  // default behavior is miss!
  val axi = Module(new AXIReadPort(addrReqWidth = addrWidth, AXIID = 0.U)) // 0 INSTR_ID

  val addrBuffer   = RegInit(0.U(addrWidth.W))
  val booting      = WireDefault(0.B) // 用于启动axi和refillBuffer组件
  val refillBuffer = Module(new ReFillBuffer)
  val refillFinal  = WireDefault(0.B)

  val idle :: refilling :: waiting :: Nil = Enum(3)
  val state                               = RegInit(idle)

  switch(state) {
    is(idle) {
      when(io.request.addr.fire) {
        state      := refilling
        addrBuffer := io.request.addr.bits
      }
    }
    is(refilling) {
      when(refillFinal) {
        state := Mux(io.request.data.fire, idle, waiting)
      }
    }
    is(waiting) {
      when(io.request.data.fire) {
        state := idle
      }
    }
  }

  // inner signal
  booting     := (io.request.addr.fire && state === idle)
  refillFinal := refillBuffer.io.dataOut.valid

  // io ready & valid
  io.request.addr.ready := (state === idle)
  io.request.data.valid := (refillFinal && state === refilling) || (state === waiting)

  // connection
  axi.io.addrReq.valid := booting
  axi.io.addrReq.bits  := addrBuffer
  axi.io.axi <> io.axi

  val bankIndex = addrBuffer(
    config.bankOffsetLen + config.bankIndexLen,
    config.bankOffsetLen + 1
  ) // (5, 3) usually
  refillBuffer.io.beginBankIndex.valid := booting
  refillBuffer.io.beginBankIndex.bits  := bankIndex
  refillBuffer.io.dataIn <> axi.io.transferData
  refillBuffer.io.dataLast := axi.io.lastBeat
  // refillBuffer.io.dataOut.ready := io.request.data.fire // tmp for now TODO

  io.request.data.bits := refillBuffer.io.dataOut.bits // tmp for now TODO
}

/**
  * Currently, ICache does not check addr.valid, data.ready.
  * Icache also asserts addr.ready at all time.
  * The only useful port is data.valid
  *
  * @param memoryFile Path to memory file.
  */
class ICache(memoryFile: String = "test_data/imem.in") extends Module {
  val io = IO(new ICacheWithCore)

  val s1  = RegInit(0.U.asTypeOf(Valid(new ICacheReq)))
  val mem = Mem(1024, UInt(dataWidth.W))
  val s2  = RegInit(0.U.asTypeOf(Valid(new ICacheResp)))

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  } else {
    println("error ifile! ")
  }

  io.req.ready := 1.B
  when(io.flush || !io.req.fire) {
    s1.valid := 0.B
  }.elsewhen(io.req.fire) {
    s1.valid := io.req.valid
    s1.bits  := io.req.bits
  }

  val s1Index = s1.bits.addr(31, 2)
  val s1Data  = (0 to 1).map(i => mem(s1Index + i.U))

  s2.valid        := Mux(io.flush, 0.B, s1.valid)
  s2.bits.data(0) := s1Data(0)
  s2.bits.data(1) := Mux(s1Index(2, 0) =/= "b111".U, s1Data(1), 0.U(dataWidth.W))
  io.resp         := s2
}

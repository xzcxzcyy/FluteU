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

  val addrBuffer      = RegInit(0.U(addrWidth.W))
  val addrValid       = WireDefault(0.B)
  val refillBuffer    = Module(new ReFillBuffer)
  val refillDataValid = WireDefault(0.B)

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
      when(axi.io.finishTransfer) {
        state := Mux(io.request.data.fire, idle, waiting)
      }
    }
    is(waiting) {
      when(io.request.data.fire) {
        state := idle
      }
    }
  }

  // io ready & valid
  io.request.addr.ready := (state === idle)
  io.request.data.valid := (axi.io.finishTransfer && state === refilling) || (state === waiting)

  // inner valid
  addrValid := (state === refilling) || (io.request.addr.fire && state === idle)

  // connection
  axi.io.addrReq.valid := addrValid
  axi.io.addrReq.bits  := addrBuffer
  axi.io.axi <> io.axi

  refillBuffer.io.beginBankIndex.valid := addrValid
  val bankIndex = addrBuffer(config.bankOffsetLen + config.bankIndexLen ,config.bankOffsetLen + 1) // (5, 3) usually
  refillBuffer.io.beginBankIndex.bits := bankIndex
  refillBuffer.io.dataIn <> axi.io.transferData
  refillBuffer.io.dataLast := axi.io.finishTransfer
  
  io.request.data.bits := refillBuffer.io.dataOut
}


/**
  * Currently, ICache does not check addr.valid, data.ready.
  * Icache also asserts addr.ready at all time.
  * The only useful port is data.valid
  *
  * @param memoryFile Path to memory file.
  */
class ICache(memoryFile: String = "test_data/imem.in") extends Module {
  val io = IO(new ICacheIO)

  val instInd = Cat(io.addr.bits(31, 2 + fetchGroupWidth), 0.U(fetchGroupWidth.W))

  val mem = Mem(1024, UInt(dataWidth.W))

  val memPorts = for (i <- 0 until fetchGroupSize) yield mem.read(instInd + i.U)

  val lastInstInd = RegInit(-1.BM.U(29, 0))
  lastInstInd := instInd

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  io.addr.ready := 1.B
  // io.data.valid := 1.B
  io.data.valid := (lastInstInd === instInd)
  for (i <- 0 to fetchGroupSize - 1) yield {
    io.data.bits(i.U) := memPorts(i)
  }
}

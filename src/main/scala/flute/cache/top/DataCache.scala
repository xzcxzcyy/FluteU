package flute.cache.top

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.config.CacheConfig
import flute.core.backend.decode.StoreMode
import flute.core.components.MuxStageReg
import flute.core.components.MuxStageRegMode
import chisel3.util.experimental.loadMemoryFromFileInline
import flute.axi.AXIIO
import flute.cache.axi.AXIRead
import flute.cache.axi.AXIWirte

/**
  * 
  * [[storeMode]] 为 [[disable]] 时，为 Load 指令。
  */
class DCacheReq extends Bundle {
  val addr      = UInt(addrWidth.W)
  val storeMode = UInt(StoreMode.width.W)
  val writeData = UInt(dataWidth.W)
}

class DCacheResp extends Bundle {
  val loadData = UInt(dataWidth.W)
}

class DCacheWithCore extends Bundle {
  val req = Flipped(DecoupledIO(new DCacheReq))

  /** valid 标志一次有效的load数据, store类型的请求不做任何回应 */
  val resp = ValidIO(new DCacheResp)

  val flush = Input(Bool())
}

class ThroughDCache extends Module {
  val io = IO(new Bundle {
    val core = new DCacheWithCore
    val axi  = AXIIO.master()
  })

  val axiRead  = Module(new AXIRead(axiId = 1.U))
  val axiWrite = Module(new AXIWirte(axiId = 1.U))

  val idle :: active :: Nil = Enum(2)
  val state                 = RegInit(idle)

  switch(state) {
    is(idle) {
      when(io.core.flush) {
        state := idle
      }.elsewhen(io.core.req.fire) {
        state := active
      }
    }

    is(active) {
      when(io.core.flush) {
        state := idle
      }.elsewhen(axiRead.io.resp.valid || axiWrite.io.resp) {
        state := idle
      }
    }
  }

  axiRead.io.req.bits  := io.core.req.bits.addr
  axiRead.io.req.valid := io.core.req.valid && io.core.req.bits.storeMode === StoreMode.disable

  axiWrite.io.req.bits.addr      := io.core.req.bits.addr
  axiWrite.io.req.bits.data      := io.core.req.bits.writeData
  axiWrite.io.req.bits.storeMode := io.core.req.bits.storeMode
  axiWrite.io.req.valid := io.core.req.valid && io.core.req.bits.storeMode =/= StoreMode.disable

  io.core.req.ready := axiRead.io.req.ready && axiWrite.io.req.ready && state === idle

  io.core.resp.bits.loadData := axiRead.io.resp.bits
  io.core.resp.valid         := axiRead.io.resp.valid && state === active

  io.axi.ar <> axiRead.io.axi.ar
  io.axi.r <> axiRead.io.axi.r
  io.axi.aw <> axiWrite.io.axi.aw
  io.axi.w <> axiWrite.io.axi.w
  io.axi.b <> axiWrite.io.axi.b

  axiRead.io.axi.aw  := DontCare
  axiRead.io.axi.w   := DontCare
  axiRead.io.axi.b   := DontCare
  axiWrite.io.axi.ar := DontCare
  axiWrite.io.axi.r  := DontCare
}

class DataCache(cacheConfig: CacheConfig, memoryFile: String) extends Module {
  val io = IO(new DCacheWithCore)

  val mem = Mem(32 * 4, UInt(byteWidth.W))
  val s0  = RegInit(0.U.asTypeOf(Valid(new DCacheReq)))
  val s1  = RegInit(0.U.asTypeOf(Valid(new DCacheResp)))

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  io.req.ready := 1.B
  when(io.req.fire && !io.flush) {
    s0.valid := 1.B
    s0.bits  := io.req.bits
  }.otherwise {
    s0.valid := 0.B
  }

  val req         = s0.bits
  val addrRounded = Cat(s0.bits.addr(31, 2), 0.U(2.W))

  val readData = Cat(
    mem(addrRounded + 3.U),
    mem(addrRounded + 2.U),
    mem(addrRounded + 1.U),
    mem(addrRounded)
  )
  val isStore = s0.bits.storeMode =/= StoreMode.disable
  val isLoad  = !isStore

  when(s0.valid && isStore) {
    // byte, halfword, word
    mem(s0.bits.addr) := s0.bits.writeData(7, 0)

    // halfword, word
    when(s0.bits.storeMode === StoreMode.halfword || s0.bits.storeMode === StoreMode.word) {
      mem(s0.bits.addr + 1.U) := s0.bits.writeData(15, 8)
    }

    // word
    when(s0.bits.storeMode === StoreMode.word) {
      mem(s0.bits.addr + 2.U) := s0.bits.writeData(23, 16)
      mem(s0.bits.addr + 3.U) := s0.bits.writeData(31, 24)
    }

  }

  when(s0.valid && isLoad && !io.flush) {
    s1.valid         := 1.B
    s1.bits.loadData := readData
  }.otherwise {
    s1.valid         := 0.B
    s1.bits.loadData := 0.U
  }
  io.resp.valid := s1.valid
  io.resp.bits  := s1.bits

}

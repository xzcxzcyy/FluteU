package flute.cache.top

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.config.CacheConfig
import flute.core.backend.decode.StoreMode
import flute.core.components.MuxStageReg
import flute.core.components.MuxStageRegMode
import chisel3.util.experimental.loadMemoryFromFileInline

/**
  * 
  * @param store 为1代表当前为store类型指令, 0代表load类型指令
  */
class DCacheReq extends Bundle {
  val addr      = UInt(addrWidth.W)
  val storeMode = UInt(StoreMode.width.W)
  val writeData = UInt(dataWidth.W)
}

class DCacheResp extends Bundle {
  val loadData = UInt(dataWidth.W)
}

class DCachePorts extends Bundle {
  val req = Flipped(DecoupledIO(new DCacheReq))

  /** valid 标志一次有效的load数据, store类型的请求不做任何回应 */
  val resp = ValidIO(new DCacheResp)

  /** 标志DCache状态机进入miss handle状态 */
  val stallReq = Output(Bool())
}

class DataCache(cacheConfig: CacheConfig, memoryFile: String) extends Module {
  val io = IO(new DCachePorts)

  val mem = Mem(32 * 4, UInt(byteWidth.W))
  val s0  = RegInit(0.U.asTypeOf(Valid(new DCacheReq)))
  val s1  = RegInit(0.U.asTypeOf(Valid(new DCacheResp)))

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  io.req.ready := 1.B
  when(io.req.fire) {
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

  when(s0.valid && isLoad) {
    s1.valid         := 1.B
    s1.bits.loadData := readData
  }.otherwise {
    s1.valid         := 0.B
    s1.bits.loadData := 0.U
  }
  io.stallReq   := 0.B
  io.resp.valid := s1.valid
  io.resp.bits  := s1.bits

}

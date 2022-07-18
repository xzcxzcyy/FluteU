package flute.cache.top

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.config.CacheConfig
import flute.core.decode.StoreMode

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

class DataCache(cacheConfig: CacheConfig) extends Module {
  val io = IO(new DCachePorts)

  // DEBUG ONLY //
  io := DontCare
  // ---------- //
}

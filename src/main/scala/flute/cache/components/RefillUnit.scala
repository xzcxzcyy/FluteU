package flute.cache.components

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.config.CacheConfig
import flute.axi.AXIIO
import flute.cache.axi.AXIReadPort

class RefillUnit(AXIID: UInt)(implicit cacheConfig: CacheConfig) extends Module {
  val io = IO(new Bundle {
    val addr = Flipped(ValidIO(UInt(addrWidth.W)))

    /** valid信号用来标记数据可用（持续1周期），用于Cache状态机转换 */
    val data = ValidIO(Vec(cacheConfig.numOfBanks, UInt(32.W)))

    val axi = AXIIO.master()
  })
  val axiReadPort  = Module(new AXIReadPort(addrWidth, AXIID)) // TODO AXIID
  val refillBuffer = Module(new ReFillBuffer)
  // outer connection
  io.axi                               <> axiReadPort.io.axi
  io.data                              := refillBuffer.io.dataOut
  axiReadPort.io.addrReq               := io.addr
  refillBuffer.io.beginBankIndex.valid := io.addr.valid
  refillBuffer.io.beginBankIndex.bits  := cacheConfig.getBankIndex(io.addr.bits)

  // inner connection
  refillBuffer.io.dataIn   := axiReadPort.io.transferData
  refillBuffer.io.dataLast := axiReadPort.io.lastBeat
}


object RefillUnitGen extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new RefillUnit(0.U)(iCacheConfig), Array("--target-dir", "target/verilog/axi", "--target:fpga"))
}
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

    /** fire信号用来标识外部信号是否成功锁存data */
    val data = DecoupledIO(Vec(cacheConfig.numOfBanks, UInt(32.W)))

    val axi = AXIIO.master()
  })
  val axiReadPort  = Module(new AXIReadPort(addrWidth, AXIID)) // TODO AXIID
  val refillBuffer = Module(new ReFillBuffer)
  // outer connection
  io.axi                               := axiReadPort.io.axi
  io.data                              := refillBuffer.io.dataOut
  axiReadPort.io.addrReq               := io.addr
  refillBuffer.io.beginBankIndex.valid := io.addr.valid
  refillBuffer.io.beginBankIndex.bits := io.addr.bits(
    cacheConfig.bankOffsetLen + cacheConfig.bankIndexLen,
    cacheConfig.bankOffsetLen + 1
  )

  // inner connection
  refillBuffer.io.dataIn   := axiReadPort.io.transferData
  refillBuffer.io.dataLast := axiReadPort.io.lastBeat
}

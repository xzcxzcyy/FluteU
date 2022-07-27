package flute.cache.axi

import chisel3._
import chisel3.util._
import flute.axi.AXIIO
import flute.config.CPUConfig._
import flute.config.CacheConfig

class AXIWirte(axiId: Int)(implicit cacheConfig: CacheConfig) extends Module {
  private val len = (cacheConfig.numOfBanks - 1)
  
  val io = IO(new Bundle {
    val req  = Flipped(DecoupledIO(UInt(addrWidth.W)))
    val data = Input(UInt((dataWidth * len).W))
    val resp = Output(Bool())

    val axi = AXIIO.master()
  })

}

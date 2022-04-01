package flute.cache.components

import chisel3._
import chisel3.util._
import flute.config.CacheConfig
import flute.config.CPUConfig._

class ReFillBuffer(implicit cacheConfig: CacheConfig) extends Module {
  val io = IO(new Bundle {
    ///  in ///
    val dataIn         = Flipped(ValidIO(UInt(32.W)))
    val dataLast       = Input(Bool())
    val beginBankIndex = Flipped(ValidIO(UInt(cacheConfig.bankIndexLen.W)))

    /// out ///
    val dataOut = ValidIO(Vec(fetchGroupSize, UInt(32.W)))
  })
  val buffer = RegInit(
    VecInit(Seq.fill(cacheConfig.numOfBanks)(0.U(cacheConfig.bankWidth.W)))
  )
  // TODO fulfill
  io.dataOut.valid := 0.B
  io.dataOut.bits  := buffer
}

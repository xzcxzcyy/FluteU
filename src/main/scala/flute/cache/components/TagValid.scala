package flute.cache.components

import chisel3._
import flute.config.CacheConfig
import flute.cache.ram.TypedSinglePortRam
import flute.cache.ram.TypedRamIO

class TagValidBundle(implicit cacheConfig: CacheConfig) extends Bundle {
  val tag   = UInt(cacheConfig.tagLen.W)
  val valid = Bool()
}
class TagValid(implicit cacheConfig: CacheConfig) extends Module {
  val numOfSets = cacheConfig.numOfSets
  val numOfWays = cacheConfig.numOfWays

  val io = IO(new Bundle {
    val select = Vec(numOfWays, new TypedRamIO(numOfSets, new TagValidBundle))
  })

  val dataFiled =
    for (i <- 0 until numOfWays) yield Module(new TypedSinglePortRam(numOfSets, new TagValidBundle))

  for (i <- 0 until numOfWays) {
    io.select(i) <> dataFiled(i).io
  }

}

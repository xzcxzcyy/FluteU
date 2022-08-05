package flute.config

import chisel3._
import chisel3.util.log2Up

object CPUConfig {
  // Interrupts
  val intrProgramAddr = 0xBFC00380L
  /// amount ///
  val regAmount     = 32 // TODO: to be refractored
  val archRegAmount = 32
  val phyRegAmount  = 64
  //// IBuffer
  val ibufferAmount = 16
  // ROB /////////////////
  val exceptionAmount = 16
  val instrTypeAmount = 8
  val robEntryAmount  = 64
  ////////////////////////
  // SBuffer //////
  val sbufferAmount = 8
  /////////////////
  val issueQEntryMaxAmount = 16

  val superscalar = 2
  val decodeWay   = 2

  val fetchGroupSize   = 2
  val fetchGroupWidth  = log2Up(fetchGroupSize)
  val fetchAmountWidth = fetchGroupWidth + 1

  /// width ///
  val instrWidth        = 32
  val dataWidth         = 32
  val addrWidth         = 32
  val byteWidth         = 8
  val regAddrWidth      = 5
  val shamtWidth        = 5
  val iTypeImmWidth     = 16
  val archRegAddrWidth  = log2Up(archRegAmount)
  val phyRegAddrWidth   = log2Up(phyRegAmount)
  val exceptionIdxWidth = log2Up(exceptionAmount)
  val instrTypeWidth    = log2Up(instrTypeAmount)
  val robEntryNumWidth  = log2Up(robEntryAmount)

  val iCacheConfig = CacheConfig(numOfSets = 64, numOfWays = 2)  // 2路组相连 2 * 2KB
  val dCacheConfig = CacheConfig(numOfSets = 128, numOfWays = 2) // 2路组相连 2 * 4KB
}

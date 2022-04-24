package flute.core.rename.freelist

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class FreeList extends Module{
  val gen = UInt(PhyRegIdxWidth.W)
  val numRead = superscalar
  val numWrite = superscalar
  val io = IO(new FIFOBundle(gen, numRead, numWrite))
  // initial, maybe all register in Queue but $0
  val fifo = new FIFOQueue(gen,PhyRegsAmout,numRead,numWrite)

  fifo.io <> io

}



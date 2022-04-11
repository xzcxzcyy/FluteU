package flute.core.issue

import chisel3._
import chisel3.util._
import flute.core.rename.RenameIO

class Dispatch extends Module{
  val io = IO(new Bundle{
    val fromRename = Flipped(new RenameIO)
    val enqData    = Flipped(Vec(2, DecoupledIO(new IQEntry)))
  })

  for(i <- 0 to 2) {
    io.enqData(i) <> io.fromRename.microOps(i)
  }
}

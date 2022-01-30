package pipeline

import chisel3._
import chisel3.util.MuxLookup
import components.Comparator
import config.CpuConfig._

class Decode extends Module{
  val io = IO(new Bundle {
    val fromIf = Input(new IfIdBundle)
    val toEx = Output(new IdExBundle)
  })

  val branchCond = UInt(branchCondWidth.W)
  val comparator = Module(new Comparator())

  val branchTaken = MuxLookup(
    key = branchCond,
    default = 0.B,
    mapping = Seq(
      BranchCond.eq   -> comparator.io.flag.equal,
      BranchCond.ge   -> !comparator.io.flag.lessS,
      BranchCond.geu  -> !comparator.io.flag.lessU,
      BranchCond.gt   -> !(comparator.io.flag.lessS | comparator.io.flag.equal),
      BranchCond.gtu  -> !(comparator.io.flag.lessU | comparator.io.flag.equal),
      BranchCond.le   -> (comparator.io.flag.lessS | comparator.io.flag.equal),
      BranchCond.leu  -> (comparator.io.flag.lessU | comparator.io.flag.equal),
      BranchCond.lt   -> comparator.io.flag.lessS,
      BranchCond.ltu  -> comparator.io.flag.lessU,
      BranchCond.ne   -> !comparator.io.flag.equal,
      BranchCond.none -> 0.B,
    )
  )
}



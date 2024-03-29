package flute.core.backend.rename

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class FreelistCommit(val nCommit: Int) extends Bundle {
  val alloc = Input(Vec(nCommit, Valid(UInt(phyRegAddrWidth.W))))
  val free  = Input(Vec(nCommit, Valid(UInt(phyRegAddrWidth.W))))
}

class Freelist(val nWays: Int, val nCommit: Int) extends Module {
  // physical Regs Index Width
  private val nPregs = phyRegAmount
  private val pregsIW = log2Ceil(nPregs)

  val io = IO(new Bundle {
    val requests = Input(Vec(nWays, Bool()))

    val allocPregs = Output(Vec(nWays, Valid(UInt(pregsIW.W))))

    // commit to arch Freelist: alloc for aFreelist; free for both aFreelist & sFreelist
    val commit = new FreelistCommit(nCommit)

    val chToArch = Input(Bool())

    val debug = new Bundle {
      val freelist    = Output(UInt(nPregs.W))
      val selMask     = Output(UInt(nPregs.W))
      val sels        = Output(Vec(nWays, UInt(nPregs.W)))
      val deallocMask = Output(UInt(nPregs.W))
      val next        = Output(UInt(nPregs.W))
    }
  })

  // 1 表示空闲； 0 表示占用。 $0 默认占用
  val sFreelist = RegInit(UInt(nPregs.W), ~(1.U(nPregs.W)))

  val sels = SelectFirstN(sFreelist, nWays)

  val selFire = Wire(Vec(nWays, Bool()))

  for (i <- 0 until nWays) {
    val canSel = sels(i).orR

    selFire(i) := canSel && io.requests(i)

    io.allocPregs(i).bits  := OHToUInt(sels(i))
    io.allocPregs(i).valid := canSel
  }

  val selMask = (sels zip selFire) map { case (s, f) => s & Fill(nPregs, f) } reduce (_ | _)
  val deallocMask =
    io.commit.free.map(d => UIntToOH(d.bits)(nPregs - 1, 0) & Fill(nPregs, d.valid)).reduce(_ | _)

  val nextSFreelist = (sFreelist & ~selMask | deallocMask) & ~(1.U(nPregs.W))

  val aFreelist = RegInit(UInt(nPregs.W), ~(1.U(nPregs.W)))

  val aAlloc =
    io.commit.alloc.map(d => UIntToOH(d.bits)(nPregs - 1, 0) & Fill(nPregs, d.valid)).reduce(_ | _)
  val aFree =
    io.commit.free.map(d => UIntToOH(d.bits)(nPregs - 1, 0) & Fill(nPregs, d.valid)).reduce(_ | _)

  val nextAFreelist = (aFreelist & ~aAlloc | aFree) & ~(1.U(nPregs.W))
  aFreelist := nextAFreelist

  sFreelist := Mux(io.chToArch, nextAFreelist, nextSFreelist)

  io.debug.freelist    := sFreelist
  io.debug.selMask     := selMask
  io.debug.sels        := sels
  io.debug.deallocMask := deallocMask
  io.debug.next        := nextSFreelist

}

object SelectFirstN {
  def apply(in: UInt, n: Int) = {
    val sels = Wire(Vec(n, UInt(in.getWidth.W)))
    var mask = in

    for (i <- 0 until n) {
      sels(i) := PriorityEncoderOH(mask)
      mask = mask & ~sels(i)
    }

    sels
  }
}

package flute.core.fetch

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import flute.cache.ICache
import flute.config.CPUConfig._
import flute.util.BitMode.fromIntToBitModeLong
import flute.core.execute.ExecuteFeedbackIO

class FetchTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Fetch instructions travelling test" in {
    test(new FetchTestTop) { c =>
      val dp = c.io.withDecode
      dp.instNum.expect(0.U)
      dp.willProcess.poke(0.U)
      c.io.state.expect(0.U)
      c.clock.step()
      c.io.feedbackFromExec.branchAddr.valid.poke(0.B)
      c.io.feedbackFromExec.branchAddr.bits.poke(0.U)
      dp.insts(0).inst.expect(0x380200f0.BM.U)
      dp.insts(1).inst.expect(0x380200f1.BM.U)
      dp.insts(2).inst.expect(0x380200f2.BM.U)
      dp.insts(3).inst.expect(0x380200f3.BM.U)
      dp.insts(4).inst.expect(0x380200f4.BM.U)
      dp.insts(5).inst.expect(0x380200f5.BM.U)
      dp.insts(6).inst.expect(0x380200f6.BM.U)
      dp.insts(7).inst.expect(0x380200f7.BM.U)
      dp.instNum.expect(8.U)
      dp.willProcess.poke(8.U)
      c.io.state.expect(0.U)
      c.clock.step(2)
      c.io.state.expect(0.U)
      dp.insts(0).inst.expect(0x380200f8.BM.U)
      dp.insts(1).inst.expect(0x380200f9.BM.U)
      dp.insts(2).inst.expect(0x380200fa.BM.U)
      dp.insts(3).inst.expect(0x380200fb.BM.U)
      dp.insts(4).inst.expect(0x380200fc.BM.U)
      dp.insts(5).inst.expect(0x380200fd.BM.U)
      dp.insts(6).inst.expect(0x380200fe.BM.U)
      dp.insts(7).inst.expect(0x380200ff.BM.U)
    }
  }
}

class FetchTestTop extends Module {
  val io = IO(new Bundle {
    val withDecode       = new FetchIO
    val feedbackFromExec = Flipped(new ExecuteFeedbackIO)
    val state            = Output(UInt(2.W))
  })
  val iCache = Module(new ICache("test_data/fetch_icache_test.in"))
  val fetch  = Module(new Fetch)
  io.state := fetch.io.state
  fetch.io.iCache <> iCache.io
  io.withDecode <> fetch.io.withDecode
  fetch.io.feedbackFromExec <> io.feedbackFromExec
}

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

  object State {
    val width         = 2
    val Free          = 0.U(width.W)
    val FirstAndBlock = 1.U(width.W)
    val Blocked       = 2.U(width.W)
    val RESERVED      = 3.U(width.W) // illegal state; do not use
  }

  "Fetch instructions travelling test" in {
    test(new FetchTestTop("test_data/fetch_icache_test.in")) { c =>
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
      c.clock.step()
      dp.instNum.expect(0.U)
      dp.willProcess.poke(0.U)
      c.clock.step()
      c.io.state.expect(0.U)
      dp.instNum.expect(8.U)
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

  "Fetch With Branch Travel Test" in {
    test(new FetchTestTop("target/clang/bubble_fetch_test.hex")) { c =>
      val clock = c.clock
      val instNum = c.io.withDecode.instNum
      val insts = c.io.withDecode.insts
      val willProcess = c.io.withDecode.willProcess
      val state = c.io.state
      val branchAddr = c.io.feedbackFromExec.branchAddr
      ///
      instNum.expect(0.U)
      state.expect(0.U)
      clock.step()
      instNum.expect(8.U)
      state.expect(State.FirstAndBlock)
      willProcess.poke(2.U)
      clock.step(2)
      willProcess.poke(0.U)
      instNum.expect(5.U)
      state.expect(State.Blocked)
      branchAddr.bits.poke(40.U)
      branchAddr.valid.poke(1.B)
      clock.step()
      branchAddr.bits.poke(0.U)
      branchAddr.valid.poke(0.B)
      instNum.expect(5.U)
      state.expect(State.Free)
      clock.step(3)
      insts(6).inst.expect(0x20010003.BM.U)
      state.expect(State.Free)
      instNum.expect(8.U)
    }
  }
}

class FetchTestTop(file: String = "test_data/fetch_icache_test.in") extends Module {
  val io = IO(new Bundle {
    val withDecode       = new FetchIO
    val feedbackFromExec = Flipped(new ExecuteFeedbackIO)
    val state            = Output(UInt(2.W))
  })
  val iCache = Module(new ICache(file))
  val fetch  = Module(new Fetch)
  io.state := fetch.io.state
  fetch.io.iCache <> iCache.io
  io.withDecode <> fetch.io.withDecode
  fetch.io.feedbackFromExec <> io.feedbackFromExec
}

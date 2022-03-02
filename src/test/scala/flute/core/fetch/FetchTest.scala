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

class FetchTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Fetch instructions travelling test" in {
    test(new FetchTestTop) { c =>
      val dp = c.io.withDecode
      dp.instNum.expect(0.U)
      dp.willProcess.poke(0.U)
      c.clock.step()
      dp.instNum.expect(8.U)
      dp.insts(0).expect(0x3801ffff.BM.U)
      dp.insts(1).expect(0x380200ff.BM.U)
      dp.willProcess.poke(8.U)
      c.clock.step()
      dp.instNum.expect(0.U)
      dp.willProcess.poke(0.U)
      c.clock.step()
      dp.instNum.expect(8.U)
      dp.insts(0).expect(0xeeeeeeee.BM.U)
      dp.insts(1).expect(0x00000001.U)
      dp.willProcess.poke(2.U)
      // another group in here...
      c.clock.step()
      dp.insts(0).expect(0x2.U)
      dp.insts(1).expect(0x3.U)
      dp.instNum.expect(6.U)
      dp.willProcess.poke(1.U)
      c.clock.step()
      dp.insts(0).expect(0x3.U)
      dp.insts(1).expect(0x4.U)
      dp.instNum.expect(8.U)
    }
  }
}

class FetchTestTop extends Module {
  val io = IO(new Bundle {
    val withDecode  = new FetchIO
  })
  val iCache = Module(new ICache("test_data/fetch_icache_test.in"))
  val fetch  = Module(new Fetch)
  fetch.io.iCache <> iCache.io
  io.withDecode <> fetch.io.withDecode
}

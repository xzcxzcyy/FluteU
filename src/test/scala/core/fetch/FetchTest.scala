package core.fetch

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import cache.ICache

class FetchTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Fetch instructions travelling test" in {
    test(new FetchTestTop) { c =>
      val dp = c.io.withDecode
      dp.instNum.expect(0.U)
      dp.willProcess.poke(0.U)
      c.clock.step()
      dp.instNum.expect(8.U)
      dp.willProcess.poke(8.U)
      c.clock.step()
      dp.instNum.expect(0.U)
      dp.willProcess.poke(0.U)
      c.clock.step()
      dp.instNum.expect(8.U)
      dp.willProcess.poke(2.U)
      // may have something wrong in here...
      c.clock.step()
      dp.instNum.expect(6.U)
      c.clock.step()
      dp.instNum.expect(8.U)
    }
  }
}

class FetchTestTop extends Module {
  val io = IO(new Bundle {
    val withDecode = new FetchIO
  })
  val iCache = Module(new ICache)
  val fetch  = Module(new Fetch)
  fetch.io.iCache <> iCache.io
  io.withDecode     <> fetch.io.next
}

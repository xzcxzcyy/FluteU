package flute.cache.top

import chisel3._
import chisel3.util._
import chiseltest._

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import flute.config.CPUConfig._
import flute.axi.AXIRam

private class TestHelper(logName: String)
    extends BaseTestHelper(logName, () => new InstrCache(iCacheConfig)) {}

class InstrCacheTest extends AnyFreeSpec with Matchers {
  "empty test" in {
    val testHelper = new TestHelper("instr_cache_empty")
    testHelper.close()
  }
}

class ThroughICacheTop extends Module {
  val io = IO(new Bundle {
    val data = ValidIO(Vec(fetchGroupSize, UInt(dataWidth.W)))
    val pc   = Output(UInt(addrWidth.W))
  })

  val throughICache = Module(new ThroughICache)
  val axiRam        = Module(new AXIRam)

  throughICache.io.axi <> axiRam.io.axi

  val pc = RegInit(0.U(addrWidth.W))

  throughICache.io.core.flush         := 0.B
  throughICache.io.core.req.bits.addr := pc
  throughICache.io.core.req.valid     := 1.B

  when(throughICache.io.core.req.fire) {
    pc := pc + 4.U
  }

  io.data.bits  := throughICache.io.core.resp.bits.data
  io.data.valid := throughICache.io.core.resp.valid

  io.pc := pc
}

class ThroughICacheTopTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Through ICache Test" in {
    test(new ThroughICacheTop)
      .withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { c =>
        for (i <- 0 until 50) {
          println(c.io.data.peek())
          c.clock.step()
        }
      }
  }
}

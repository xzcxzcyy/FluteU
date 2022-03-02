package flute.core

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import flute.cache.ICache
import flute.config.CPUConfig._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

class CoreTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Ideal Tests" in {
    test(new CoreTester) { core =>
      def step(i : Int = 1) = core.clock.step(i)
      core.io.rFdebug(1).expect(0.U)
      core.io.rFdebug(2).expect(0.U)
      core.clock.step(6 + 7)
      core.io.rFdebug(1).expect(0xffff.U)
      core.io.rFdebug(2).expect(0x00ff.U)
      core.io.rFdebug(3).expect(0xff00.U)
    }
  }
}

class CoreTester extends Module {
  val io = IO(new Bundle {
    val rFdebug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })
  val iCache = Module(new ICache("test_data/xor.in"))
  val core   = Module(new Core)
  io.rFdebug := core.io.rFdebug
  core.io.dCache := DontCare
  core.io.iCache <> iCache.io
}

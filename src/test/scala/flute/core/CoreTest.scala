package flute.core

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import flute.cache.ICache
import flute.cache.DCache
import flute.config.CPUConfig._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

class CoreTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  // "beq_bne.test" in {
  //   test(new CoreTester("target/clang/beq_bne.hexS")) { c =>
  //     val rf = c.io.rFdebug
  //     rf(0).expect(0.U)
  //     c.clock.step(50)
  //     rf(0).expect(0.U)
  //     rf(16).expect(1.U)
  //     rf(17).expect(2.U)
  //     rf(18).expect(3.U)
  //     rf(19).expect(4.U)
  //   }
  // }

  // "sw_flat.test" in {
  //   test(new CoreTester("target/clang/sw_flat.hexS")) { c =>
  //     val rf = c.io.rFdebug
  //     rf(4).expect(0.U)
  //     c.clock.step(20)
  //     rf(4).expect(0x1234.U)
  //   }
  // }

  "sb_flat.test" in {
    test(new CoreTester("target/clang/sb_flat.hexS")) { c => 
      val rf = c.io.rFdebug
      rf(4).expect(0.U)
      c.clock.step(20)
      rf(4).expect(0x00000100.U)
    }
  }
}

class CoreTester(memoryFile: String = "") extends Module {
  val io = IO(new Bundle {
    val rFdebug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })
  val iCache = Module(new ICache(memoryFile))
  val dCache = Module(new DCache("test_data/zero.in")) // TODO: Specify cache file here
  val core   = Module(new Core)
  io.rFdebug := core.io.rFdebug
  core.io.dCache <> dCache.io.port
  core.io.iCache <> iCache.io
}

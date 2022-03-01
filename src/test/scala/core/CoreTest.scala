package core

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import cache.ICache
import config.CPUConfig._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

class CoreTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Ideal Tests" in {
    test(new Core) { core =>
    }
  }
}

class CoreTester extends Module {
  val io = IO(new Bundle {
    val debug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })
  val iCache = Module(new ICache)
  val core   = Module(new Core)
  io.debug := core.io.debug
  core.io.dCache := DontCare
  core.io.iCache <> iCache.io
}

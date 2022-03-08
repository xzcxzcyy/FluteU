package flute.core

import chisel3._
import treadle.TreadleTester

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.ChiselScalatestTester

import firrtl.stage.FirrtlSourceAnnotation
import firrtl.options.TargetDirAnnotation

import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

class CoreTreadleTester extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "s1_base" in {
    val tester = new CoreTestHelper("s1_base")

    tester.step(8)

    tester.report()
  }

  "beq_bne" in {
    val tester = new CoreTestHelper("beq_bne")

    tester.step(64)

    for(i <- 1 to 4) {
      tester.expect(s"io_rFdebug_${i + 15}", i)
    }

    tester.report()
  }
}

class CoreTestHelper(hexCode:String) {
  val firrtlAnno = (new ChiselStage).execute(
    Array(),
    Seq(
      TargetDirAnnotation("target"),
      ChiselGeneratorAnnotation(() => new CoreTester(s"target/clang/${hexCode}.hexS"))
    )
  )

  val tester = TreadleTester(firrtlAnno)
  var clock = 0

  def printClock() = {
    println(s"================ clock $clock ================")
  }

  def peek(port:String, format:String = "") = {
    val value = tester.peek(port)
    if(format.length > 0) {
      println(s"$port: ${format.format(value)}")
    } else {
      println(s"$port: ${value}")
    }
  }

  def displayPorts() = {
    printClock()
    for (i <- 0 until 2) {
      peek(s"core.fetch.io_withDecode_ibufferEntries_${i}_bits_inst", "0x%08x")
      peek(s"core.fetch.io_withDecode_ibufferEntries_${i}_bits_addr", "0x%08x")
      peek(s"core.fetch.io_withDecode_ibufferEntries_${i}_valid")
    }
    peek("core.fetch.state")
    for (i <- 0 until 32) {
      peek(s"io_rFdebug_${i}", "0x%08x")
    }
  }

  def expect(port:String, value:BigInt) = {
    tester.expect(port, value)
  }

  def step(tick:Int = 1) = {
    tester.step(tick)
    clock += tick
  }

  def stepAndDisplay(tick:Int) = {
    for(i <- 0 until tick) {
      step()
      displayPorts()
    }
  }

  def report() = {
    tester.report()
  }
}
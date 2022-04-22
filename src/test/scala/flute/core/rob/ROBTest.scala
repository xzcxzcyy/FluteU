package flute.core.rob

import flute.util.BaseTestHelper
import firrtl.AnnotationSeq
import treadle.TreadleTester

import chisel3._
import chisel3.util._
import chisel3.stage._
import firrtl.options.TargetDirAnnotation
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import flute.config.CPUConfig._

private class TestHelper(fileName: String)
    extends BaseTestHelper(fileName, () => new ROB(32, 2, 2, 2)) {
  def printWritePort() = {
    for (i <- 0 to 1) {
      val robAddr    = t.peek(s"io_write_${i}_robAddr")
      val writeReady = t.peek(s"io_write_${i}_ready") 
      writer.println(s"Write #$i: ready=${writeReady} robAddr=${"0x%08x".format(robAddr)}")
    }
  }

  def pokeWritePort(en: Boolean, ind: Int = 0, pc: Int = 0) = {
    t.poke(s"io_write_${ind}_bits_pc", BigInt(pc))
    t.poke(s"io_write_${ind}_valid", bool2BigInt(en))
    t.poke(s"io_write_${ind}_bits_complete", BigInt(0))
  }

  def peekReadPort() = {
    for (i <- 0 to 1) {
      val pc = t.peek(s"io_read_${i}_bits_pc")
      val valid = t.peek(s"io_read_${i}_valid")
      val complete = t.peek(s"io_read_${i}_bits_complete")
      writer.println(s"Read #$i: valid=${valid} pc=${"0x%08x".format(pc)} complete=${complete}")
    }
  }

  def pokeReadPort(en: Boolean, ind: Int) = {
    t.poke(s"io_read_${ind}_ready", bool2BigInt(en))
  }
}

class ROBTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "rob general test" in {
    val tester = new TestHelper("rob_general")
    for (i <- 0 until 1) yield {
      tester.step(1)
      tester.printWritePort()
    }
    tester.close()
  }
}

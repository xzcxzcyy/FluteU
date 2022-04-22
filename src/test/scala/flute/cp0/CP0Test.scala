package flute.cp0

import chisel3._
import treadle.TreadleTester

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.ChiselScalatestTester

import firrtl.stage.FirrtlSourceAnnotation
import firrtl.options.TargetDirAnnotation
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

import java.io.PrintWriter
import java.io.File
import flute.util.BaseTestHelper

class TestHelper(logName: String) extends BaseTestHelper(s"cp0/${logName}") {

  override val firrtlAnno = (new ChiselStage).execute(
    Array(),
    Seq(
      TargetDirAnnotation("target"),
      ChiselGeneratorAnnotation(() => new CP0)
    )
  )

  override val t = TreadleTester(firrtlAnno)

  def printCP0Regs() = {
    val badvaddr = t.peek("io_debug_badvaddr")
    val count    = t.peek("io_debug_count")
    val status   = t.peek("io_debug_status")
    val cause    = t.peek("io_debug_cause")
    val epc      = t.peek("io_debug_epc")
    val compare  = t.peek("io_debug_compare")
    fprintln(s"BadVAddr: ${"0x%08x".format(badvaddr)}")
    fprintln(s"Count: ${"0x%08x".format(count)}")
    fprintln(s"Status: ${bigIntToBitmode(status)}")
    fprintln(s"Cause : ${bigIntToBitmode(cause)}")
    fprintln(s"Epc: ${"0x%08x".format(epc)}")
    fprintln(s"Compare: ${"0x%08x".format(compare)}")
  }

  def bigIntToBitmode(b: BigInt, leastLength: Int = 32) = {
    val raw = b.toString(2)
    val pad = if (leastLength - raw.length() > 0) {
      leastLength - raw.length()
    } else {
      0
    }
    s"${"0" * pad}${raw}"
  }

  def printIntrReq() = {
    val intrReq = t.peek("io_core_intrReq")
    fprintln(s"IntrReqOut: ${intrReq == 1}")
  }

  def printStatusAll() = {
    printCP0Regs()
    printIntrReq()
  }

  def pokeWrite(addr: Int, sel: Int, data: Int, enable: Boolean = true) = {
    t.poke("io_core_write_addr", BigInt(addr))
    t.poke("io_core_write_sel", BigInt(sel))
    t.poke("io_core_write_enable", if (enable) BigInt(1) else BigInt(0))
    t.poke("io_core_write_data", BigInt(data))
  }

  def pokeWrite(enable: Boolean) = {
    t.poke("io_core_write_enable", if (enable) BigInt(1) else BigInt(0))
  }
}

class CP0Test extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "timing test" in {
    val t = new TestHelper("Timing")
    t.pokeWrite(CP0Compare.addr, CP0Compare.sel, 10, true)
    t.step(1)
    t.pokeWrite(false)
    t.pokeWrite(CP0Status.addr, CP0Status.sel, Integer.parseInt("00000000010000001000000000000001", 2))
    t.step()
    t.pokeWrite(false)
    for (i <- 0 until 64) yield {
      t.step()
      t.printStatusAll()
    }
    t.close()
  }

  "hw4 test" in {
    val t = new TestHelper("HW4")
    t.pokeWrite(CP0Status.addr, CP0Status.sel, Integer.parseInt("00000000010000000100000000000001", 2))
    t.printStatusAll()
    t.step()
    t.pokeWrite(false)
    t.poke("io_hwIntr", BigInt.apply("010000", 2))
    t.printStatusAll()
    t.step()
    t.printStatusAll()
    // t.poke("io_hwIntr", 0)
    t.step()
    for (i <- 0 until 16) {
      t.printStatusAll()
      t.step()
    }
    t.close()
  }
}

package flute.core

import flute.cache.ICache
import flute.cache.DCache
import flute.config.CPUConfig._

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

class TestHelper(bench: String, d: String = "zero.in") {

  val f = new File(s"target/log/$bench.log")
  f.getParentFile().mkdirs();
  val w = new PrintWriter(f)

  println(s"=================== $bench ===================")
  w.println(s"=================== $bench ===================")

  val firrtlAnno = (new ChiselStage).execute(
    Array(),
    Seq(
      TargetDirAnnotation("target"),
      ChiselGeneratorAnnotation(() => new CoreTester(bench, d))
    )
  )

  val t     = TreadleTester(firrtlAnno)
  val poke  = t.poke _
  val peek  = t.peek _
  var clock = 0
  def step(n: Int = 1) = {
    t.step(n)
    clock += n
    w.println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
    println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
  }
  def printRFs() = {
    w.println("pc=" + "0x%08x".format(peek("core.fetch.pc.io_out")))
    w.println("status=" + "%d".format(peek("core.fetch.state")))
    val regFileDebug = for (i <- 0 until regAmount) yield {
      peek(s"io_debug_$i")
    }
    w.println("RegFile")
    for (i <- 0 until 8) {
      val msg = regFileDebug.slice(i * 4, i * 4 + 4).foldLeft("")(_ + "\t" + "0x%08x".format(_))
      w.println(msg)
    }
  }
  def smToString(storeMode: Int) = storeMode match {
    case 0 => "disable"
    case 1 => "word"
    case 2 => "byte"
    case 3 => "halfword"
    case _ => "illegal"
  }
  def lmToString(loadMode: Int) = loadMode match {
    case 0 => "disable"
    case 1 => "word"
    case 2 => "byte"
    case 3 => "halfword"
    case _ => "illegal"
  }
  def printDecodeOut() = {
    for (i <- 0 until decodeWay) yield {
      val valid = peek(s"core.decode.io_withExecute_microOps_${i}_valid")
      val loadMode = lmToString(
        peek(s"core.decode.io_withExecute_microOps_${i}_bits_loadMode").toInt
      )
      val storeMode = smToString(
        peek(s"core.decode.io_withExecute_microOps_${i}_bits_storeMode").toInt
      )
      w.println(s"valid #${i}: ${valid}")
      w.println(s"loadMode #${i}: ${loadMode}")
      w.println(s"storeMode #${i}: ${storeMode}")
    }
  }
  def printDCacheInput() = {
    for (i <- 0 until superscalar) yield {
      val storeMode = smToString(peek(s"dCache.io_port_${i}_storeMode").toInt)
      val writeData = "0x%08x".format(peek(s"dCache.io_port_${i}_writeData"))
      val addr      = "0x%08x".format(peek(s"dCache.io_port_${i}_addr"))
      w.println(s"dCache storeMode peeked: #${i}: ${storeMode}")
      w.println(s"dCache writeData peeked: #${i}: ${writeData}")
      w.println(s"dCache addr peeked: #${i}: ${addr}")
    }
  }

  def close() = {
    w.close()
  }
}

class CoreTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "beq_bne" in {
    val tester = new TestHelper("beq_bne")
    for (i <- 0 until 32) {
      tester.step()
      tester.printRFs()
    }
    tester.close()
  }

  "s1_base" in {
    val tester = new TestHelper("s1_base")
    for (i <- 0 until 0) {
      tester.step()
      tester.printRFs()
    }
    tester.close()
  }

  "s4_loadstore" in {
    val tester = new TestHelper("s4_loadstore")
    for (i <- 0 until 0) {
      tester.step()
      tester.printRFs()
    }
    tester.close()
  }

  "sb_flat" in {
    val tester = new TestHelper("sb_flat")
    for (i <- 0 until 0) {
      tester.step()
      tester.printRFs()
    }
    tester.close()
  }

  "sb" in {
    val tester = new TestHelper("sb")
    for (i <- 0 until 0) {
      tester.step()
      tester.printRFs()
    }
    tester.close()
  }

  "sort" in {
    val tester = new TestHelper("sort", "sort.dcache")
    for (i <- 0 until 0) {
      tester.step()
      tester.printRFs()
    }
    tester.close()
  }

}

class CoreTester(i: String = "", d: String = "zero.in") extends Module {
  val io = IO(new Bundle {
    val debug = Output(Vec(regAmount, UInt(dataWidth.W)))
  })
  val iCache = Module(new ICache(s"target/clang/${i}.hexS"))
  val dCache = Module(new DCache(s"test_data/${d}")) // TODO: Specify cache file here
  val core   = Module(new Core)

  io.debug := core.io.debug

  core.io.dCache <> dCache.io.port
  core.io.iCache <> iCache.io
}

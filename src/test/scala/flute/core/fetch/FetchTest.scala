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
import flute.core.execute.ExecuteFeedbackIO
import firrtl.options.TargetDirAnnotation
import chisel3.stage.ChiselGeneratorAnnotation
import treadle.TreadleTester
import chisel3.stage.ChiselStage

class FetchTest extends AnyFreeSpec with Matchers with ChiselScalatestTester {
  "Fetch1: base" in {
    println("=================== Fetch1: base ===================")

    val firrtlAnno = (new ChiselStage).execute(
      Array(),
      Seq(
        TargetDirAnnotation("target"),
        ChiselGeneratorAnnotation(() => new FetchTestTop("fetch1_base"))
      )
    )

    val t     = TreadleTester(firrtlAnno)
    val poke  = t.poke _
    val peek  = t.peek _
    var clock = 0
    def step(n: Int = 1) = {
      t.step(n)
      clock += n
      println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
    }
    def displayReadPort() = {
      for (i <- 0 until 2) {
        val instruction = peek(s"io_withDecode_ibufferEntries_${i}_bits_inst")
        val address     = peek(s"io_withDecode_ibufferEntries_${i}_bits_addr")
        val valid       = peek(s"io_withDecode_ibufferEntries_${i}_valid")
        println(s"inst #$i: ${"%08x".format(instruction)}")
        println(s"addr #$i: ${"%08x".format(address)}")
        println(s"valid #$i: ${valid}")
      }
      println("state: " + peek(s"fetch.state"))
    }

    step()
    displayReadPort()

    for (j <- 0 until 8) {
      for (i <- 0 until 2) {
        poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
      }
      step(1)
      for (i <- 0 until 2) {
        poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
      }
      displayReadPort()
    }

    poke(s"io_feedbackFromExec_branchAddr_valid", 1)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0x4c)
    step(1)
    poke(s"io_feedbackFromExec_branchAddr_valid", 0)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0)

    displayReadPort()

    for (j <- 0 until 8) {
      for (i <- 0 until 2) {
        poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
      }
      step(1)
      for (i <- 0 until 2) {
        poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
      }
      displayReadPort()
    }

    step(1)

    displayReadPort()

    t.report()
  }

  "beq bne" in {
    println("=================== beq bne ===================")

    val firrtlAnno = (new ChiselStage).execute(
      Array(),
      Seq(
        TargetDirAnnotation("target"),
        ChiselGeneratorAnnotation(() => new FetchTestTop("beq_bne"))
      )
    )

    val t     = TreadleTester(firrtlAnno)
    val poke  = t.poke _
    val peek  = t.peek _
    var clock = 0
    def step(n: Int = 1) = {
      t.step(n)
      clock += n
      println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
    }
    def displayReadPort() = {
      for (i <- 0 until 2) {
        val instruction = peek(s"io_withDecode_ibufferEntries_${i}_bits_inst")
        val address     = peek(s"io_withDecode_ibufferEntries_${i}_bits_addr")
        val valid       = peek(s"io_withDecode_ibufferEntries_${i}_valid")
        println(s"inst #$i: ${"%08x".format(instruction)}")
        println(s"addr #$i: ${"%08x".format(address)}")
        println(s"valid #$i: ${valid}")
      }
      println("state: " + peek(s"fetch.state"))
    }

    step()
    displayReadPort()

    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
    }
    step()
    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
    }
    displayReadPort()

    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
    }
    step()
    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
    }
    displayReadPort()

    poke(s"io_feedbackFromExec_branchAddr_valid", 1)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0x14)
    step(1)
    poke(s"io_feedbackFromExec_branchAddr_valid", 0)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0)
    displayReadPort()

    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
    }
    step()
    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
    }
    displayReadPort()

    step()

    displayReadPort()

    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
    }
    step()
    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
    }
    displayReadPort()

    poke(s"io_feedbackFromExec_branchAddr_valid", 1)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0x28)
    step(1)
    poke(s"io_feedbackFromExec_branchAddr_valid", 0)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0)
    displayReadPort()

    step(2)

    displayReadPort()
    
    poke(s"io_feedbackFromExec_branchAddr_valid", 1)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0x30)
    step(1)
    poke(s"io_feedbackFromExec_branchAddr_valid", 0)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0)
    displayReadPort()

    step(2)

    displayReadPort()

    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
    }
    step()
    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
    }
    displayReadPort()

    poke(s"io_feedbackFromExec_branchAddr_valid", 1)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0x44)
    step(1)
    poke(s"io_feedbackFromExec_branchAddr_valid", 0)
    poke(s"io_feedbackFromExec_branchAddr_bits", 0)
    displayReadPort()

    step(2)

    displayReadPort()

    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 1)
    }
    step()
    for (i <- 0 until 2) {
      poke(s"io_withDecode_ibufferEntries_${i}_ready", 0)
    }
    displayReadPort()
  }
}

class FetchTestTop(file: String = "") extends Module {
  val io = IO(new Bundle {
    val withDecode       = new FetchIO
    val feedbackFromExec = Flipped(new ExecuteFeedbackIO)
  })
  val iCache = Module(new ICache(s"target/clang/${file}.hexS"))
  val fetch  = Module(new Fetch)
  fetch.io.iCache <> iCache.io
  io.withDecode <> fetch.io.withDecode
  fetch.io.feedbackFromExec <> io.feedbackFromExec
}

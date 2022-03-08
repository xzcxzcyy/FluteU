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
    val firrtlAnno = (new ChiselStage).execute(
        Array(),
        Seq(
            TargetDirAnnotation("target"),
            ChiselGeneratorAnnotation(() => new CoreTester("target/clang/s1_base.hexS"))
        )
    )

    val tester = TreadleTester(firrtlAnno)
    var clock = 1

    def displayReadPort() = {
      for (i <- 0 until 2) {
        val instruction = tester.peek(s"core.fetch.io_withDecode_ibufferEntries_${i}_bits_inst")
        val address     = tester.peek(s"core.fetch.io_withDecode_ibufferEntries_${i}_bits_addr")
        val valid       = tester.peek(s"core.fetch.io_withDecode_ibufferEntries_${i}_valid")
        println(s"inst #$i: ${"%08x".format(instruction)}")
        println(s"addr #$i: ${"%08x".format(address)}")
        println(s"valid #$i: ${valid}")
      }
      println("state: " + tester.peek(s"core.fetch.state"))
    }

    for(tick <- 1 to 5) {
        tester.step()
        println(s"================ clock $clock ================")
        val pc = tester.peek("core.fetch.pc.io_out")
        println(s"pc: ${pc}")

        displayReadPort()

        clock += 1
    }

    tester.report()
  }
}
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
            ChiselGeneratorAnnotation(() => new CoreTester("s1_base"))
        )
    )

    val tester = TreadleTester(firrtlAnno)
    var clock = 1

    for(tick <- 1 to 5) {
        tester.step()
        println(s"================ clock $clock ================")
        val pc = tester.peek("core.fetch.pc.io_out")
        println(s"pc: ${pc}")
        for(i <- 0 to 7) {
            val inst = tester.peek(s"iCache.io_data_bits_$i")
            println(s"fetch_$i: ${"%08x".format(inst)}")
        }

        val willProcess = tester.peek(s"core.fetch.io_withDecode_willProcess")
        println(s"willProcess: $willProcess")

        for(i <- 0 to 1) {
            val inst = tester.peek(s"core.decode.io_withFetch_insts_${i}_inst")
            println(s"decode_$i: ${"%08x".format(inst)}")
        }
        clock += 1
    }

    tester.report()
  }
}
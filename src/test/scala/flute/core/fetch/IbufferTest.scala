package flute.core.fetch

import chisel3._
import treadle.TreadleTester

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.ChiselScalatestTester

import firrtl.stage.FirrtlSourceAnnotation
import firrtl.options.TargetDirAnnotation

import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

class IbufferTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test 1" in {
    val firrtlAnno = (new ChiselStage).execute(
      Array(),
      Seq(
        TargetDirAnnotation("target"),
        ChiselGeneratorAnnotation(() => new Ibuffer(UInt(32.W), 16, 2, 8))
      )
    )

    val tester = TreadleTester(firrtlAnno)
    tester.report()
  }
}

object IbufferFirrtlTester extends App {
  val firrtlAnno = (new ChiselStage).execute(
    Array(),
    Seq(
      TargetDirAnnotation("target"),
      ChiselGeneratorAnnotation(() => new Ibuffer(UInt(32.W), 16, 2, 8))
    )
  )

  val tester = TreadleTester(firrtlAnno)
  val poke = tester.poke _
  var clock  = 0
  def step(n: Int = 1) = {
    clock += n
    tester.step(n)
  }
  def printElements() = {
    println(s"================ total clock steped: ${clock} ================")
    for (i <- 0 until 16) {
      println(s"data_${i}\t" + tester.peek(s"data_${i}"))
    }
  }

  step()

  printElements()

  for (i <- 1 to 7) {
    poke(s"io_write_${i}_bits", i)
    poke(s"io_write_${i}_valid", 1)
  }
  step()
  for (i <- 1 to 7) {
    poke(s"io_write_${i}_bits", 0)
    poke(s"io_write_${i}_valid", 0)
  }

  printElements()

  step()

  printElements()
  
  tester.report()
}

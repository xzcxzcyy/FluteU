package flute.core.frontend

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
  val peek   = tester.peek _
  val poke   = tester.poke _
  var clock  = 0
  def printRead(i: Int) = {
    println(
      s"io_read_${i}_bits = " +
        tester.peek(s"io_read_${i}_bits") +
        s"; io_read_${i}_valid = " +
        tester.peek(s"io_read_${i}_valid")
    )
  }
  def step(n: Int = 1) = {
    clock += n
    tester.step(n)
    println(s"================ total clock steped: ${clock} ================")
  }
  def printElements() = {
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

  for (i <- 3 to 7) {
    poke(s"io_write_${i}_bits", i * 10)
    poke(s"io_write_${i}_valid", 1)
  }
  step()
  for (i <- 3 to 7) {
    poke(s"io_write_${i}_bits", 0)
    poke(s"io_write_${i}_valid", 0)
  }

  printElements()

  for (i <- 2 to 6) {
    poke(s"io_write_${i}_bits", i * 100)
    poke(s"io_write_${i}_valid", 1)
  }
  for (i <- 0 to 1) {
    poke(s"io_read_${i}_ready", 1)
    printRead(i)
  }
  step()
  for (i <- 2 to 6) {
    poke(s"io_write_${i}_bits", 0)
    poke(s"io_write_${i}_valid", 0)
  }
  for (i <- 0 to 1) {
    poke(s"io_read_${i}_ready", 0)
  }

  printElements()

  for (i <- 1 to 5) {
    poke(s"io_write_${i}_bits", i * 1000)
    poke(s"io_write_${i}_valid", 1)
  }
  step()
  for (i <- 1 to 5) {
    poke(s"io_write_${i}_bits", i * 1000)
    poke(s"io_write_${i}_valid", 0)
  }

  printElements()

  for (i <- 0 to 1) {
    if (i == 0) {
      poke(s"io_read_${i}_ready", 1)
    } else {
      poke(s"io_read_${i}_ready", 0)
    }
    printRead(i)
  }
  step()
  for (i <- 0 to 1) {
    poke(s"io_read_${i}_ready", 0)
  }

  for (j <- 0 until 10) {
    for (i <- 0 to 1) {
      poke(s"io_read_${i}_ready", 1)
      printRead(i)
    }
    step()
    for (i <- 0 to 1) {
      poke(s"io_read_${i}_ready", 0)
    }
  }

  printElements()

  for (i <- 0 to 7) {
    poke(s"io_write_${i}_bits", i * 10000)
    poke(s"io_write_${i}_valid", 1)
  }
  step()
  for (i <- 0 to 7) {
    poke(s"io_write_${i}_bits", 0)
    poke(s"io_write_${i}_valid", 0)
  }

  printElements()

  for (j <- 0 until 10) {
    for (i <- 0 to 1) {
      poke(s"io_read_${i}_ready", 1)
      printRead(i)
    }
    step()
    for (i <- 0 to 1) {
      poke(s"io_read_${i}_ready", 0)
    }
  }

  printElements()

  tester.report()
}

package flute.util

import chisel3._
import treadle.TreadleTester

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.ChiselScalatestTester

import firrtl.stage.FirrtlSourceAnnotation
import firrtl.options.TargetDirAnnotation

import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import scala.math.BigInt

class FIFOTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  case class In(writeValid:Boolean, readReady:Boolean, writeBits:Int = 0)
  case class Out(writeReady:Boolean, readValid:Boolean, readBits:Int = 0)

  class FIFOTestHelper(withWriteBuffer:Boolean = false) {
    val firrtlAnno = (new ChiselStage).execute(
      Array(),
      Seq(
        TargetDirAnnotation("target"),
        ChiselGeneratorAnnotation(() => new FIFOQueue(UInt(32.W), 8, 2, 2, withWriteBuffer))
      )
    )

    val fifo = TreadleTester(firrtlAnno)
 

    def toInt(b:Boolean):Int = {
      if(b) 1 else 0
    }

    var clock = 0
    def peek(names:String*) = {
      println(s"---------------- $clock ----------------")
      names.foreach(name => {
        val value = fifo.peek(name)
        println(s"${name}: ${value}")
      })
    }

    def test(in:Seq[In], out:Seq[Out]) = {
      for (i <- 0 until 2) {
        fifo.poke(s"io_write_${i}_valid", toInt(in(i).writeValid))
        fifo.poke(s"io_read_${i}_ready", toInt(in(i).readReady))
        if(in(i).writeValid) {
          fifo.poke(s"io_write_${i}_bits", in(i).writeBits)
        }
      }
      peek("head_ptr", "tail_ptr", "numValid", "maxEnq", "numEnq", "maxDeq", "numDeq")
      fifo.step()
      clock += 1
      for (i <- 0 until 2) {
        fifo.expect(s"io_write_${i}_ready", toInt(out(i).writeReady))
        fifo.expect(s"io_read_${i}_valid", toInt(out(i).readValid))
        if(out(i).readValid) {
          fifo.expect(s"io_read_${i}_bits", out(i).readBits)
        }
      }
    }
  }

  "fifo without write buffer" in {
    val fifo = new FIFOTestHelper()
    fifo.test(Seq(In(true, false, 1), In(true, false, 2)), Seq(Out(true, false), Out(true, false)))
    fifo.test(Seq(In(true, false, 3), In(true, false, 4)), Seq(Out(true, false), Out(true, false)))
    fifo.test(Seq(In(true, false, 5), In(true, false, 6)), Seq(Out(true, false), Out(true, false)))
    fifo.test(Seq(In(true, false, 7), In(true, false, 8)), Seq(Out(true, false), Out(false, false)))
    fifo.test(Seq(In(false, false, 9), In(true, false, 8)), Seq(Out(true, false), Out(false, false)))
    fifo.test(Seq(In(false, true, 9), In(true, false, 8)), Seq(Out(true, true, 1), Out(false, false)))
    fifo.test(Seq(In(false, true, 9), In(true, false, 8)), Seq(Out(true, true, 2), Out(true, false)))
    fifo.test(Seq(In(true, true, 9), In(true, true, 10)), Seq(Out(true, true, 3), Out(false, true, 4)))
    fifo.test(Seq(In(true, true, 10), In(true, true, 11)), Seq(Out(true, true, 5), Out(true, true, 6)))
    fifo.test(Seq(In(false, true), In(false, true)), Seq(Out(true, true, 7), Out(true, true, 8)))
    fifo.test(Seq(In(false, true), In(false, true)), Seq(Out(true, true, 9), Out(true, true, 10)))
    fifo.test(Seq(In(false, true), In(false, true)), Seq(Out(true, true, 11), Out(true, false)))
    fifo.test(Seq(In(true, true, 12), In(true, true, 13)), Seq(Out(true, true, 12), Out(true, true, 13)))
    fifo.test(Seq(In(true, true, 14), In(true, true, 15)), Seq(Out(true, true, 14), Out(true, true, 15)))
    fifo.test(Seq(In(true, true, 16), In(true, true, 17)), Seq(Out(true, true, 16), Out(true, true, 17)))
    fifo.test(Seq(In(true, true, 12), In(true, true, 13)), Seq(Out(true, true, 12), Out(true, true, 13)))
    fifo.test(Seq(In(true, true, 14), In(true, true, 15)), Seq(Out(true, true, 14), Out(true, true, 15)))
    fifo.test(Seq(In(true, true, 16), In(true, true, 17)), Seq(Out(true, true, 16), Out(true, true, 17)))
  }

  "fifo with write buffer" in {
    val fifo = new FIFOTestHelper(true)

    fifo.test(Seq(In(true, false, 1), In(true, false, 2)), Seq(Out(true, false), Out(true, false)))
    fifo.test(Seq(In(true, false, 3), In(true, false, 4)), Seq(Out(true, false), Out(true, false)))
    fifo.test(Seq(In(true, false, 5), In(true, false, 6)), Seq(Out(true, false), Out(true, false)))
    fifo.test(Seq(In(true, false, 7), In(true, false, 8)), Seq(Out(true, false), Out(false, false)))
    fifo.test(Seq(In(false, false, 9), In(true, false, 8)), Seq(Out(true, false), Out(false, false)))
    fifo.test(Seq(In(false, true, 9), In(true, false, 8)), Seq(Out(true, true, 1), Out(false, false)))
    fifo.test(Seq(In(false, true, 9), In(true, false, 8)), Seq(Out(true, true, 2), Out(true, false)))
    fifo.test(Seq(In(true, true, 9), In(true, true, 10)), Seq(Out(true, true, 3), Out(false, true, 4))) // insert 10 fault but hold in write buffer
    fifo.test(Seq(In(true, true, 10), In(true, true, 11)), Seq(Out(true, true, 5), Out(true, true, 6))) // another 10, ignore 11
    fifo.test(Seq(In(false, true), In(false, true)), Seq(Out(true, true, 7), Out(true, true, 8)))
    fifo.test(Seq(In(false, true), In(false, true)), Seq(Out(true, true, 9), Out(true, true, 10)))
    fifo.test(Seq(In(false, true), In(false, true)), Seq(Out(true, true, 10), Out(true, false))) // diff 11 -> 10
    fifo.test(Seq(In(true, true, 12), In(true, true, 13)), Seq(Out(true, true, 12), Out(true, true, 13)))
    fifo.test(Seq(In(true, true, 14), In(true, true, 15)), Seq(Out(true, true, 14), Out(true, true, 15)))
    fifo.test(Seq(In(true, true, 16), In(true, true, 17)), Seq(Out(true, true, 16), Out(true, true, 17)))
    fifo.test(Seq(In(true, true, 12), In(true, true, 13)), Seq(Out(true, true, 12), Out(true, true, 13)))
    fifo.test(Seq(In(true, true, 14), In(true, true, 15)), Seq(Out(true, true, 14), Out(true, true, 15)))
    fifo.test(Seq(In(true, true, 16), In(true, true, 17)), Seq(Out(true, true, 16), Out(true, true, 17)))
  }
}

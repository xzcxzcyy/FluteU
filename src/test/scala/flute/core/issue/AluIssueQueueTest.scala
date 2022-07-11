package flute.core.issue

import chisel3._
import chisel3.util._

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest._
import flute.core.decode.MicroOp
import scala.collection.mutable.ListBuffer

private class AluIssueQueueTestHelper(fileName: String)
    extends BaseTestHelper(
      fileName,
      () => new AluIssueQueue(20, 4)
    ) {}

class AluIssueQueueTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "compile" in {
    val t = new AluIssueQueueTestHelper("AluIssueQueueTestHelper")
    val res = t.t.peek(s"io_data_0_valid")
    t.writer.println(res)
    t.writer.close()
  }
  "all test" in {
    val detectWidth = 20
    test(new AluCompressIssueQueue(UInt(32.W), 20, detectWidth)) { c =>
      def expectData(a: Seq[Int]) = {
        for (i <- 0 until a.length) {
          c.io.data(i).valid.expect(1.B)
          c.io.data(i).bits.expect(a(i).U)
        }
        for (i <- a.length until detectWidth) {
          c.io.data(i).valid.expect(0.B)
        }
      }
      def expectReady() = {
        c.io.enq(0).ready.expect(1.B)
        c.io.enq(1).ready.expect(1.B)
      }

      def enq(a: Seq[Int]) = {
        assert(a.length <= 2)
        for (i <- 0 until a.length) {
          c.io.enq(i).valid.poke(1.B)
          c.io.enq(i).bits.poke(a(i).U)

        }
        for (i <- a.length until 2) {
          c.io.enq(i).valid.poke(0.B)
        }
      }

      def issue(a: Seq[Int]) = {
        assert(a.length <= 2)
        for (i <- 0 until a.length) {
          c.io.issue(i).valid.poke(1.B)
          c.io.issue(i).bits.poke(a(i).U)
        }

        for (i <- a.length until 2) {
          c.io.issue(i).valid.poke(0.B)
        }
      }

      def init() = {
        issue(Seq())
        enq(Seq(1, 2))
        c.clock.step()
        expectData(Seq(1, 2))

        enq(Seq(3, 4))
        expectReady()
        c.clock.step()

        enq(Seq(5, 6))
        expectReady()
        c.clock.step()

        enq(Seq(7, 8))
        expectReady()
        c.clock.step()

        expectData(Seq(1, 2, 3, 4, 5, 6, 7, 8))
      }

      init()

      enq(Seq())
      issue(Seq(1, 3))
      c.clock.step()

      expectData(Seq(1, 3, 5, 6, 7, 8))

      enq(Seq())
      issue(Seq(2))

      c.clock.step()

      expectData(Seq(1, 3, 6, 7, 8))

      c.io.flush.poke(1.B)
      c.clock.step()

      expectData(Seq())

      c.io.flush.poke(0.B)
      init()

      enq(Seq(9, 10))
      issue(Seq(0, 4))
      c.clock.step()
      expectData(Seq(2, 3, 4, 6, 7, 8, 9, 10))

      c.io.flush.poke(1.B)
      enq(Seq())
      issue((Seq()))
      c.clock.step()
      c.io.flush.poke(0.B)
      for (i <- 0 until 9) {
        enq(Seq(1, 1))
        c.clock.step()
      }
      expectReady()

      enq(Seq(1))
      c.clock.step()
      
      c.io.enq(0).ready.expect(1.B)
      c.io.enq(1).ready.expect(0.B)

      enq(Seq(1, 2))
      c.clock.step()

      c.io.enq(0).ready.expect(0.B)
      c.io.enq(1).ready.expect(0.B)

      expectData(Seq.fill(20)(1))

    }
  }
}

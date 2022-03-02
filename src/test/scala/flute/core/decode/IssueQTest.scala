package flute.core.decode

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

class IssueQTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "enq&issue test" in {
    test(new TestIssueQ) { c =>
      def step() = c.clock.step()
      def enq2data(addr1: Int, data1: Int, addr2: Int, data2: Int) = {
        c.io.enqAddr(0).valid.poke(1.B)
        c.io.enqAddr(1).valid.poke(1.B)
        c.io.enqAddr(0).bits.poke(addr1.U)
        c.io.enqAddr(1).bits.poke(addr2.U)
        c.io.enqData(0).valid.poke(1.B)
        c.io.enqData(1).valid.poke(1.B)
        c.io.enqData(0).bits.poke(data1.U)
        c.io.enqData(1).bits.poke(data2.U)
      }
      def setNoIssue() = {
        c.io.issueAddr(0).valid.poke(0.B)
        c.io.issueAddr(0).valid.poke(0.B)
      }
      def setNoEnq() = {
        c.io.enqAddr(0).valid.poke(0.B)
        c.io.enqAddr(1).valid.poke(0.B)
      }
      def expectNum(num: Int) = {
        c.io.entryNum.expect(num.U)
      }
      def issueOne(addr: Int) = {
        c.io.issueAddr(0).valid.poke(1.B)
        c.io.issueAddr(0).bits.poke(addr.U)
      }
      def issueTwo(addr1: Int, addr2: Int) = {
        c.io.issueAddr(0).valid.poke(1.B)
        c.io.issueAddr(0).bits.poke(addr1.U)
        c.io.issueAddr(1).valid.poke(1.B)
        c.io.issueAddr(1).bits.poke(addr2.U)
      }
      def expectData(a: Array[Int]) = {
        for (i <- 0 until a.length) {
          c.io.dataOut(i).expect(a(i).U)
        }
      }

      setNoIssue()
      enq2data(0, 1, 1, 2)
      step()
      enq2data(2, 3, 3, 4)
      step()
      enq2data(4, 5, 5, 6)
      step()

      expectNum(6)
      expectData(Array(1, 2, 3, 4, 5, 6))

      issueTwo(1, 3)
      enq2data(4, 7, 5, 8)
      step()
      expectNum(6)
      expectData(Array(1, 3, 5, 6, 7, 8))

    }
  }
}

class TestIssueQ extends IssueQ(UInt(32.W))

class IdeaIssueQueueTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new TestIdeaQ) { c =>
      c.io.in(0).bits.poke(1.U)
      c.io.in(0).valid.poke(1.B)
      c.io.in(1).bits.poke(2.U)
      c.io.in(1).valid.poke(1.B)

      c.clock.step()

      c.io.out(0).bits.expect(1.U)
      c.io.out(1).bits.expect(2.U)
    }
  }
}

class TestIdeaQ extends IdeaIssueQueue(UInt(32.W))

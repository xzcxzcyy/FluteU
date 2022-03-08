package flute.core.decode

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

class CompressQTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new CompressQ(UInt(32.W))) { c =>
      def step() = c.clock.step()
      def setNoIssue() = {
        c.io.issueAddr(0).valid.poke(0.B)
        c.io.issueAddr(1).valid.poke(0.B)
      }
      def enq2data(data0: Int, data1: Int) = {
        c.io.enqData(0).valid.poke(1.B)
        c.io.enqData(0).bits.poke(data0.U)
        c.io.enqData(1).valid.poke(1.B)
        c.io.enqData(1).bits.poke(data1.U)
      }
      def expectData(a: Array[Int]) = {
        for (i <- 0 until a.length) {
          c.io.dataOut(i).expect(a(i).U)
        }
      }
      def expectNum(num: Int) = {
        c.io.entryNum.expect(num.U)
      }
      def issueTwo(addr1: Int, addr2: Int) = {
        c.io.issueAddr(0).valid.poke(1.B)
        c.io.issueAddr(0).bits.poke(addr1.U)
        c.io.issueAddr(1).valid.poke(1.B)
        c.io.issueAddr(1).bits.poke(addr2.U)
      }
      def issueOne(addr: Int) = {
        c.io.issueAddr(1).valid.poke(0.B)
        c.io.issueAddr(0).valid.poke(1.B)
        c.io.issueAddr(0).bits.poke(addr.U)
      }

      setNoIssue()
      expectNum(0)
      enq2data(1, 2)
      c.io.ctrl(0).expect(c.MoveState.readFirst)
      c.io.ctrl(1).expect(c.MoveState.readSecond)
      step()
      expectNum(2)
      expectData(Array(1, 2))

      enq2data(3, 4)
      step()
      enq2data(5, 6)
      step()
      enq2data(7, 8)
      issueOne(1)
      step()
      expectData(Array(1, 3, 4, 5, 6, 7, 8))

      enq2data(9, 10)
      issueTwo(1, 3)
      step()
      expectData(Array(1, 4, 6, 7, 8, 9, 10))

    }
  }
}

package flute.core.decode.issue

import chisel3._
import treadle.TreadleTester
import chiseltest._

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.ChiselScalatestTester

import flute.core.decode.issue.WBconfig

class WritingBoardTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new WritingBoard) { w =>
      // query bind
      for (i <- 0 until WBconfig.queryPort) {
        w.io.query.addrIn(i).poke(i.U)
      }

      def expect(arr: Seq[Int]) = {
        for (i <- 0 until WBconfig.queryPort) {
          w.io.query.dataOut(i).expect(arr(i).U)
        }
      }

      w.io.checkIn(0).valid.poke(1.B)
      w.io.checkIn(1).valid.poke(1.B)
      w.io.checkIn(0).bits.poke(0.U)
      w.io.checkIn(1).bits.poke(1.U)

      w.io.checkOut(0).valid.poke(0.B)
      w.io.checkOut(1).valid.poke(0.B)

      expect(Seq(0, 0, 0, 0, 0))

      w.clock.step()

      expect(Seq(1, 1, 0, 0, 0))

      w.clock.step()
      w.clock.step()
      expect(Seq(3, 3, 0, 0, 0))

      w.io.checkIn(0).valid.poke(0.B)
      w.io.checkOut(0).valid.poke(1.B)
      w.io.checkOut(1).valid.poke(1.B)

      w.io.checkOut(0).bits.poke(0.U)
      w.io.checkOut(1).bits.poke(1.U)

      w.clock.step()
      expect(Seq(2, 3, 0, 0, 0))

    }

  }
}

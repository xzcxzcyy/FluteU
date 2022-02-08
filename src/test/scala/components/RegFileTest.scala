package components

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chisel3.experimental.VecLiterals._

class RegFileTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Register File Test" in {
    test(new RegFile()) { s =>
      s.io.writeEnable.poke(1.B)
      s.io.writeData.poke(1037.U(32.W))
      s.io.writeAddr.poke(0.U(5.W))
      s.io.r1Addr.poke(0.U(5.W))
      s.clock.step()
      s.io.r1Data.expect(0.U(32.W))
      ////
      s.io.writeEnable.poke(1.B)
      s.io.writeData.poke(1037.U(32.W))
      s.io.writeAddr.poke(3.U(5.W))
      s.io.r1Addr.poke(3.U(5.W))
      s.clock.step()
      s.io.r1Data.expect(1037.U(32.W))
    }
  }

  "Debug port test" in {
    test(new RegFile()) { s =>
      val regAmount = 32
      val dataWidth = 32
      var array = (0 until regAmount).map(i => 0.U(dataWidth.W)).toVector
      s.io.debug.expect(Vec.Lit(array: _*))
      s.io.writeAddr.poke(3.U)
      s.io.writeData.poke(1035.U)
      s.io.writeEnable.poke(1.B)
      s.clock.step()
      array = array.updated(3, 1035.U(dataWidth.W))
      s.io.debug.expect(Vec.Lit(array: _*))
    }
  }
}

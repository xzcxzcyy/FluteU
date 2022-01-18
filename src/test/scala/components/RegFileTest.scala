package components

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

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
}

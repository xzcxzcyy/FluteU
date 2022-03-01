package core.components

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chisel3.experimental.VecLiterals._
import core.components.RegFile

class RegFileTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "General Test" in {
    test(new RegFile(2, 2)) { c =>
      for (i <- 0 until 2) {
        val pos = i + 1
        c.io.write(i).writeAddr.poke(pos.U)
        c.io.write(i).writeEnable.poke(1.B)
        c.io.write(i).writeData.poke(pos.U)
      }
      c.clock.step()
      for (i <- 0 until 2) {
        val pos = i + 3
        c.io.write(i).writeAddr.poke(pos.U)
        c.io.write(i).writeEnable.poke(1.B)
        c.io.write(i).writeData.poke(pos.U)
      }
      c.clock.step()
      c.io.read(0).r1Addr.poke(1.U)
      c.io.read(0).r2Addr.poke(2.U)
      c.io.read(1).r1Addr.poke(3.U)
      c.io.read(1).r2Addr.poke(4.U)

      c.io.read(0).r1Data.expect(1.U)
      c.io.read(0).r2Data.expect(2.U)
      c.io.read(1).r1Data.expect(3.U)
      c.io.read(1).r2Data.expect(4.U)
    }
  }

  /**
    * ATTENTION: DO NOT DO THIS; BEHAVIOR IS UNDEFINED.
    */
  "Write hazard test" in {
    test(new RegFile(2, 2)) { c =>
      c.io.write(0).writeEnable.poke(1.B)
      c.io.write(0).writeAddr.poke(1.U)
      c.io.write(0).writeData.poke(1111.U)

      c.io.write(1).writeEnable.poke(1.B)
      c.io.write(1).writeAddr.poke(1.U)
      c.io.write(1).writeData.poke(2222.U)

      c.clock.step()
      // 实验结果罢了
      c.io.debug(1).expect(2222.U)
    }
  }

  // "Debug port test" in {
  //   test(new RegFile()) { s =>
  //     val regAmount = 32
  //     val dataWidth = 32
  //     var array = (0 until regAmount).map(i => 0.U(dataWidth.W)).toVector
  //     s.io.debug.expect(Vec.Lit(array: _*))
  //     s.io.writeAddr.poke(3.U)
  //     s.io.writeData.poke(1035.U)
  //     s.io.writeEnable.poke(1.B)
  //     s.clock.step()
  //     array = array.updated(3, 1035.U(dataWidth.W))
  //     s.io.debug.expect(Vec.Lit(array: _*))
  //   }
  // }
}

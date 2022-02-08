package cpu

import chisel3._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import fluteutil.BitMode.fromIntToBitModeLong
import config.CpuConfig._

class CPUTopTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "alu port test" in {
    test(new CPUTop()) { u =>
      var array = (0 until regAmount).map(i => 0.U(dataWidth.W))
      println(array)
      // u.decode.regFile.io.debug.expect(Vec(regAmount, UInt(dataWidth.W)).Lit(array: _*))
      u.decode.regFile.io.debug.expect(Vec.Lit(array: _*))
      // array = array.updated(0, (0, 1.U))
    }
  }
}
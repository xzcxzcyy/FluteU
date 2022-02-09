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
    test(new CPUTop) { u =>
      var array = (0 until regAmount).map(i => 0.U(dataWidth.W))
      def expect() = {
        u.io.regfileDebug.expect(Vec.Lit(array: _*))
      }
      def update(i: Int, newValue: Long) = {
        array = array.updated(i, newValue.U(dataWidth.W))
      }
      expect()
    }
  }

  "general tests" in {
    test(new CPUTop) { u =>
      var array = (0 until regAmount).map(i => 0.U(dataWidth.W))
      def expect() = {
        u.io.regfileDebug.expect(Vec.Lit(array: _*))
      }
      def update(i: Int, newValue: Long) = {
        array = array.updated(i, newValue.U(dataWidth.W))
      }
      expect()
      u.clock.step(100)
      update(1, 0xffff.BM)
      update(2, 0xff.BM)
      update(3, 0xff00.BM)
      expect()

    /** 
      * Usage:
      * Write instructions into test_data/imem.in; data into test_data/dmem.in;
      * Call update to modify expected regfile contents. 
      * Call expect to assert these values
      * 
      * ATTENTION
      * Redirection is NOT finished yet. Insert nop operations in test_data/imem.in manually
      *   to avoid hazard.
      */
    }
  }
}

package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import config.CpuConfig._

class ExMemStageTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "EX/MEM Stage Test" in {
    test(new ExMemStage()) { s =>
      val data = s.io.data
      val io = s.io
      io.flush.poke(1.B)
      io.in.control.memToReg.poke(0.B)
      s.clock.step()
      ///
      data.control.memToReg.expect(0.B)
      data.writeData.expect(0.U)
      ///
      io.in.aluResult.poke(28.U)
      io.in.control.memWrite.poke(1.B)
      io.flush.poke(0.B)
      io.valid.poke(1.B)
      s.clock.step()
      ///
      data.control.memWrite.expect(1.B)
      data.aluResult.expect(28.U)
    }
  }
}
package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import config.CpuConfig._

class IdExStageTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "ID/EX Stage Test" in {
    test(new IdExStage()) { s =>
      val data = s.io.data
      val io = s.io
      io.flush.poke(1.B)
      s.clock.step()
      data.control.aluOp.expect(0.U(aluOpWidth.W))
      data.control.aluSrc.expect(0.B)
      data.immediate.expect(0.U)
      io.in.control.aluOp.poke(1.U)
      io.flush.poke(0.B)
      io.valid.poke(1.B)
      io.in.rs.poke(28.U)
      s.clock.step()
      data.control.aluOp.expect(1.U)
      data.rs.expect(28.U)
    }
  }
}


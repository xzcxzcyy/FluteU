package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class IfIdStageTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "IF/ID Stage Test" in {
    test(new IfIdStage()) { s =>
      s.io.flush.poke(0.B)
      s.io.valid.poke(1.B)
      s.io.in.instruction.poke(0x1BBBBBBB.U(32.W))
      s.io.in.pcplusfour.poke(8.U(32.W))
      s.clock.step()
      s.io.data.instruction.expect(0x1BBBBBBB.U(32.W))
      s.io.data.pcplusfour.expect(8.U(32.W))
      ////
      s.io.valid.poke(0.B)
      s.io.in.instruction.poke(0x3BBBBBBB.U(32.W))
      s.clock.step()
      s.io.data.instruction.expect(0x1BBBBBBB.U(32.W))
      ////
      s.io.flush.poke(1.B)
      s.clock.step()
      s.io.data.instruction.expect(0.U(32.W))
      s.io.data.pcplusfour.expect(0.U(32.W))
      ////
      s.io.flush.poke(1.B)
      s.io.valid.poke(1.B)
      s.io.in.instruction.poke(555.U(32.W))
      s.io.in.pcplusfour.poke(555.U(32.W))
      s.clock.step()
      s.io.data.instruction.expect(0.U(32.W))
      s.io.data.pcplusfour.expect(0.U(32.W))
    }
  }
}


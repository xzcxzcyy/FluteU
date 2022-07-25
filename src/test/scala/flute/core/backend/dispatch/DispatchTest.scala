package flute.core.backend.dispatch

import chisel3._
import chisel3.util._
import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import chiseltest._
import flute.core.backend.decode._
import flute.core.components.ALUOp

class DispatchTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "0->ls; 1->alu" in {
    test(new Dispatch()) { c =>
      c.io.in(0).valid.poke(1.B)
      c.io.in(1).valid.poke(1.B)
      c.io.in(0).bits.instrType.poke(InstrType.loadStore)
      c.io.in(1).bits.instrType.poke(InstrType.alu)

      c.io.out(0).valid.expect(1.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)

      c.io.out(0).bits.instrType.expect(InstrType.alu)
      c.io.out(2).bits.instrType.expect(InstrType.loadStore)
    }
  }
  "0->alu; 1->ls" in {
    test(new Dispatch()) { c =>
      c.io.in(0).valid.poke(1.B)
      c.io.in(1).valid.poke(1.B)
      c.io.in(0).bits.instrType.poke(InstrType.alu)
      c.io.in(1).bits.instrType.poke(InstrType.loadStore)

      c.io.out(0).valid.expect(1.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)

      c.io.out(0).bits.instrType.expect(InstrType.alu)
      c.io.out(2).bits.instrType.expect(InstrType.loadStore)
    }
  }

  "dual alu" in {
    test(new Dispatch()) { c =>
      c.io.in(0).valid.poke(1.B)
      c.io.in(1).valid.poke(1.B)
      c.io.in(0).bits.instrType.poke(InstrType.alu)
      c.io.in(0).bits.aluOp.poke(ALUOp.add)
      c.io.in(1).bits.instrType.poke(InstrType.alu)
      c.io.in(1).bits.aluOp.poke(ALUOp.addu)

      c.io.out(0).valid.expect(1.B)
      c.io.out(1).valid.expect(1.B)
      c.io.out(2).valid.expect(0.B)
      c.io.out(3).valid.expect(0.B)

      c.io.out(0).bits.aluOp.expect(ALUOp.add)
      c.io.out(1).bits.aluOp.expect(ALUOp.addu)
    }
  }

  "0->ls; 1->mdu" in {
    test(new Dispatch()) { c =>
      c.io.in(0).valid.poke(1.B)
      c.io.in(1).valid.poke(1.B)
      c.io.in(0).bits.instrType.poke(InstrType.loadStore)
      c.io.in(1).bits.instrType.poke(InstrType.mulDiv)

      c.io.out(0).valid.expect(0.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(1.B)

      c.io.out(2).bits.instrType.expect(InstrType.loadStore)
      c.io.out(3).bits.instrType.expect(InstrType.mulDiv)
    }
  }

  "dual ls: ready all the time" in {
    test(new Dispatch()) { c =>
      c.io.in(0).valid.poke(1.B)
      c.io.in(1).valid.poke(1.B)
      c.io.in(0).bits.instrType.poke(InstrType.loadStore)
      c.io.in(0).bits.pc.poke(0.U)
      c.io.in(1).bits.instrType.poke(InstrType.loadStore)
      c.io.in(1).bits.pc.poke(4.U)

      c.io.out(2).ready.poke(1.B)

      //---------step0--------------//
      c.io.out(0).valid.expect(0.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)
      c.io.stallReq.expect(1.B)
      c.io.out(2).bits.pc.expect(0.U)
      //---------step0--------------//

      c.clock.step()

      c.io.out(2).ready.poke(1.B)
      //---------step1--------------//
      c.io.out(0).valid.expect(0.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)
      c.io.stallReq.expect(0.B)
      c.io.out(2).bits.pc.expect(4.U)
      //---------step1--------------//

      
      c.clock.step()
    }
  }

  "dual ls: not ready" in {
    test(new Dispatch()) { c =>
      c.io.in(0).valid.poke(1.B)
      c.io.in(1).valid.poke(1.B)
      c.io.in(0).bits.instrType.poke(InstrType.loadStore)
      c.io.in(0).bits.pc.poke(0.U)
      c.io.in(1).bits.instrType.poke(InstrType.loadStore)
      c.io.in(1).bits.pc.poke(4.U)
 
      c.io.out(2).ready.poke(0.B)
      //---------step0--------------//
      c.io.out(0).valid.expect(0.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)
      c.io.stallReq.expect(1.B)
      c.io.out(2).bits.pc.expect(0.U)
      //---------step0--------------//

      c.clock.step()

      c.io.out(2).ready.poke(1.B)
      
      //---------step1--------------//
      c.io.out(0).valid.expect(0.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)
      c.io.stallReq.expect(1.B)
      c.io.out(2).bits.pc.expect(0.U)
      //---------step1--------------//
 
      c.clock.step()

      c.io.out(2).ready.poke(0.B)

      //---------step2--------------//
      c.io.out(0).valid.expect(0.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)
      c.io.stallReq.expect(1.B)
      c.io.out(2).bits.pc.expect(4.U)
      //---------step2--------------//

      c.clock.step()

      c.io.out(2).ready.poke(1.B)

      //---------step3--------------//
      c.io.out(0).valid.expect(0.B)
      c.io.out(1).valid.expect(0.B)
      c.io.out(2).valid.expect(1.B)
      c.io.out(3).valid.expect(0.B)
      c.io.stallReq.expect(0.B)
      c.io.out(2).bits.pc.expect(4.U)
      //---------step3--------------//

    }
  }
}

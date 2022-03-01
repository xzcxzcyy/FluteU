package core.execute

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import config.CPUConfig._


class ExecuteTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
    "Execute datapath test" in {
        test(new Execute) { c =>
            c.clock.step()
            c.io.withDecode.microOps(0).bits.rs.poke(1.U)
            c.io.withDecode.microOps(0).bits.rt.poke(1.U)
            c.io.withDecode.microOps(0).bits.controlSig.aluOp.poke(10.U)
            c.io.withDecode.microOps(0).valid.poke(1.B)
        
            c.clock.step()
            c.io.withRegFile(0).writeData.expect(2.U)
        }
    }
}


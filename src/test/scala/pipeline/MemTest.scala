package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import config.CpuConfig.StoreMode

class MemTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Load test" in {
    test(new MemoryAccess()) { c =>
      c.io.fromEx.control.memToReg.poke(1.B)
      c.io.fromEx.control.regWriteEn.poke(1.B)
      c.io.fromEx.control.storeMode.poke(StoreMode.disable)
      c.io.fromEx.aluResult.poke(4.U)
      c.io.fromEx.memWriteData.poke(1.U)
      c.io.fromEx.writeRegAddr.poke(1.U)
      c.io.dMemStallReq.expect(1.B)
      c.clock.step()
      c.io.dMemStallReq.expect(0.B)
      c.io.toWb.dataFromMem.expect(0xe5L.U)
    }
  }

  "Save test" in {
    test(new MemoryAccess()) { c =>
      c.io.fromEx.control.memToReg.poke(0.B)
      c.io.fromEx.control.regWriteEn.poke(0.B)
      c.io.fromEx.control.storeMode.poke(StoreMode.word)
      c.io.fromEx.aluResult.poke(4.U)
      c.io.fromEx.memWriteData.poke(0xf5L.U)
      c.io.fromEx.writeRegAddr.poke(0.U)
      c.io.dMemStallReq.expect(0.B)
      c.clock.step()
      c.io.dMemStallReq.expect(0.B)

      c.io.fromEx.control.memToReg.poke(1.B)
      c.io.fromEx.control.regWriteEn.poke(1.B)
      c.io.fromEx.control.storeMode.poke(StoreMode.disable)
      c.io.fromEx.aluResult.poke(4.U)
      c.io.fromEx.memWriteData.poke(1.U)
      c.io.fromEx.writeRegAddr.poke(1.U)
      c.io.dMemStallReq.expect(1.B)
      c.clock.step()
      c.io.dMemStallReq.expect(0.B)
      c.io.toWb.dataFromMem.expect(0xf5L.U)
    }
  }
}

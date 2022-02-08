package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class FetchTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "No jump Test" in {
    test(new Fetch()) { c =>
      val step = () => c.clock.step()

      c.io.branchTaken.poke(false.B)
      c.io.pcStall.poke(false.B)

      c.io.iMemStallReq.expect(false.B)
      c.io.toDecode.pcplusfour.expect((0 + 4).U)
      c.io.toDecode.instruction.expect((0x55).U)

      // step 1
      step()
      c.io.iMemStallReq.expect(true.B)
      c.io.toDecode.pcplusfour.expect((4 + 4).U)

      c.io.pcStall.poke(true.B)

      // step 2
      step()
      c.io.iMemStallReq.expect(false.B)
      c.io.toDecode.pcplusfour.expect((4 + 4).U)
      c.io.toDecode.instruction.expect((0xe5).U)

    }
  }

  "Jump Test" in {
    test(new Fetch()) { c =>
      val step = () => c.clock.step()

      c.io.pcStall.poke(false.B)
      c.io.branchTaken.poke(true.B)
      c.io.branchAddr.poke(12.U)

      step()

      c.io.iMemStallReq.expect(true.B)
      c.io.toDecode.pcplusfour.expect((12 + 4).U)

      c.io.pcStall.poke(true.B)

      step()

      c.io.iMemStallReq.expect(false.B)
      c.io.toDecode.pcplusfour.expect((12 + 4).U)
      c.io.toDecode.instruction.expect((0x04).U)

    }
  }
}

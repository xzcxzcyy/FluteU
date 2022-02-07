package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class FetchTest extends AnyFreeSpec with ChiselScalatestTester with Matchers{
  "No jump Test" in {
      test(new Fetch()) { c =>
        val step = () => c.clock.step()

        c.io.branchTaken.poke(false.B)
        c.io.pcEnable.poke(true.B)

        c.io.toDecode.pcplusfour.expect(4.U)

        // step 1
        step()
        c.io.toDecode.instruction.expect((0x55).U)
        c.io.toDecode.pcplusfour.expect(8.U)

        // step 2
        step()
        c.io.toDecode.instruction.expect((0xe5).U)
        c.io.toDecode.pcplusfour.expect(12.U)

        // step 3
        step()

      }
  }

  "Jump Test" in {
      test(new Fetch()) { c =>
          val step = () => c.clock.step()

          c.io.pcEnable.poke(true.B)
          c.io.branchTaken.poke(false.B)
          // 假设 pc == 0 为跳转指令

          /// step 1
          step()
          // 此时ID周期解析为跳转指令 pc == 4
          c.io.toDecode.instruction.expect((0x55).U)

          c.io.branchTaken.poke(true.B)
          c.io.branchAddr.poke(12.U)

          /// step 2
          step()
          // 发生跳转 pc == 12
          c.io.toDecode.pcplusfour.expect((12 + 4).U)
          c.io.toDecode.instruction.expect((0xe5).U)

          // 非跳转指令
          c.io.branchTaken.poke(false.B)

          /// step 3
          step()
          c.io.toDecode.pcplusfour.expect((16 + 4).U)
          c.io.toDecode.instruction.expect((0x04).U)

          /// step 4
          step()
      }
  }
}

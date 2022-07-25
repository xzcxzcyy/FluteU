package flute.core.components

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import flute.core.backend.decode.StoreMode
import sys.process._

class SbufferTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  private val shouldName = "store_buffer_single"

  it should shouldName in {
    test(new Sbuffer).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        // VerilatorBackendAnnotation,
      )
    ) { c =>
      c.io.flush.poke(0.B)
      c.io.read.memGroupAddr.poke(1.U)
      for (i <- 0 until 6) {
        c.io.write.valid.poke(1.B)
        c.clock.step(1)
        c.io.write.valid.poke(0.B)
        c.io.retire.poke(1.B)
        c.clock.step(1)
        c.io.retire.poke(0.B)
      }
      // Write to 6
      c.io.write.valid.poke(1.B)
      c.io.write.storeMode.poke(StoreMode.word)
      c.io.write.memAddr.poke("b100".U)
      c.io.write.memData.poke("haabbccdd".U)
      c.clock.step()
      // c.io.write.valid.poke(0.B)

      // Write to 7
      c.io.write.storeMode.poke(StoreMode.byte)
      c.io.write.memAddr.poke("b111".U)
      c.io.write.memData.poke("h11".U)
      c.clock.step()

      // Write to 0
      c.io.write.storeMode.poke(StoreMode.halfword)
      c.io.write.memAddr.poke("b100".U)
      c.io.write.memData.poke("h2233".U)
      c.clock.step()

      // Write to 1
      c.io.write.storeMode.poke(StoreMode.halfword)
      c.io.write.memAddr.poke("b110".U)
      c.io.write.memData.poke("h4455".U)
      c.clock.step()

      // Write to 2
      c.io.write.storeMode.poke(StoreMode.word)
      c.io.write.memAddr.poke("b100".U)
      c.io.write.memData.poke("h66778899".U)
      c.clock.step()
      // s"sed -i -e 1,2d test_run_dir/should_${shouldName}/BetaTop.vcd".!
    }
  }
}

package flute

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sys.process._
import flute.axi.AXIRam

class FluteTopWrap extends Module {
  val io = IO(new Bundle {
    val hwIntr = Input(UInt(6.W))
    val pc     = Output(UInt(addrWidth.W))
    val arf    = Output(Vec(archRegAmount, UInt(dataWidth.W)))
    val count  = Output(UInt(dataWidth.W))
  })

  val fluteU = Module(new FluteTop)
  val axiRam = Module(new AXIRam)

  fluteU.io.axi <> axiRam.io.axi

  fluteU.io.hwIntr := io.hwIntr
  io.pc            := fluteU.io.pc
  io.arf           := fluteU.io.arf
  io.count         := fluteU.io.count
}

class FluteTopTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  it should "final" in {
    test(new FluteTopWrap).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { c =>
      for (i <- 0 until 1000) {
        c.io.hwIntr.poke(0.U)
        println(c.io.pc.peek())
        c.clock.step()
      }
      s"sed -i -e 1,2d test_run_dir/should_final/FluteTop.vcd".!
    }
  }
}

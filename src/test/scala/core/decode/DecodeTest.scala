package core.decode

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import core.fetch.FetchTestTop

import config.CPUConfig._
import core.components.{RegFile, RegFileWriteIO}

class DecodeTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    test(new IFIDTop) { c =>

			// 测试说明： aluOp = 5 应当是空指令, aluOp = 3 为xor指令
			// 见 src/test/clang/xor.c
      val d = c.io.withExecute.microOps
			d(0).bits.controlSig.aluOp.expect(5.U)
			d(1).bits.controlSig.aluOp.expect(5.U)
			d(0).valid.expect(0.B)
			c.clock.step()
			d(0).bits.controlSig.aluOp.expect(5.U)
			d(1).bits.controlSig.aluOp.expect(5.U)
			c.clock.step()
			d(0).bits.controlSig.aluOp.expect(3.U)
			d(0).bits.immediate.expect(0xffff.U)
			d(0).valid.expect(1.B)
			d(1).bits.controlSig.aluOp.expect(3.U)
			d(1).bits.immediate.expect(0x00ff.U)
			d(1).valid.expect(1.B)
			c.clock.step()
			d(0).bits.controlSig.aluOp.expect(5.U)
			d(1).bits.controlSig.aluOp.expect(5.U)
			c.clock.step()
			d(0).bits.controlSig.aluOp.expect(5.U)
			d(1).bits.controlSig.aluOp.expect(3.U)
    }
  }
}

class IFIDTop extends Module {
  val io = IO(new Bundle {
    val withExecute = new DecodeIO()
		val regFileWrite = Vec(superscalar, new RegFileWriteIO())
  })

  val fecth  = Module(new FetchTestTop)
  val decode = Module(new Decode)

  fecth.io.withDecode <> decode.io.withFetch
  io.withExecute <> decode.io.withExecute
	io.regFileWrite <> decode.io.regFileWrite
	
}

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

			///
			d(0).bits.writeRegAddr.expect(1.U)
			d(0).bits.controlSig.loadMode.expect(0.B)

			d(1).bits.controlSig.aluOp.expect(3.U)
			d(1).bits.immediate.expect(0x00ff.U)
			d(1).valid.expect(1.B)

			///
			d(1).bits.writeRegAddr.expect(2.U)

			c.clock.step()
			d(0).bits.controlSig.aluOp.expect(5.U)
			d(1).bits.controlSig.aluOp.expect(5.U)
			c.clock.step()
			d(0).bits.controlSig.aluOp.expect(5.U)
			d(1).bits.controlSig.aluOp.expect(3.U)
    }
  }

	"test2" in {
		test(new Decode) { c=>
			c.io.regFileWrite(0).writeEnable.poke(1.B)
			c.io.regFileWrite(0).writeAddr.poke(2.U)
			c.io.regFileWrite(0).writeData.poke(0xff.U)

			c.io.debug(1).expect(0.U)

			c.clock.step()

			c.io.debug(2).expect(0xff.U)
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

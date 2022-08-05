package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class FakeMDUTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "mdu test" in {
    test(new FakeMDU).withAnnotations(Seq(WriteVcdAnnotation)) { c => 
      c.io.in.valid.poke(1.B)
      c.io.in.bits.mul.poke(0.B)
      c.io.in.bits.signed.poke(1.B)
      c.io.in.bits.op1.poke("hffffffe0".U)
      c.io.in.bits.op2.poke(3.U)  
      c.io.flush.poke(0.B)

      for(i <- 0  until 5) {
        c.io.flush.poke(0.B)
        val lo = c.io.res.bits.lo.peek()
        val hi = c.io.res.bits.hi.peek()
        println(c.io.res.valid.peek())
        println(hi)
        println(lo)
        if(i == 3) c.io.flush.poke(1.B)
        c.clock.step()
      }
    }
  }
}

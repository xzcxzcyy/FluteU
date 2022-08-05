package flute.util

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import flute.cp0.ExceptionBundle


class AddrMapTestTop extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })
  io.out := AddrMap.map(io.in)
}

class CastTestTop extends Module {
  val io = IO(new Bundle {
    val in  = Input(new ExceptionBundle)
    val valid = Input(Bool())
    val completed = Input(Bool())
    val out = Output(new ExceptionBundle)
  })

  val exceptions = io.in
  io.out := VecInit(exceptions.asUInt.asBools.map(_ && io.valid && io.completed)).asTypeOf(new ExceptionBundle)
}

class ChiselMultTop extends Module {
  val io = IO(new Bundle {
    val op1 = Input(UInt(32.W))
    val op2 = Input(UInt(32.W))
    val res = Output(UInt(64.W))
    val ress = Output(SInt(64.W))
    val hi  = Output(UInt(32.W))
    val lo  = Output(UInt(32.W))
  })
  val res = io.op1.asSInt * io.op2.asSInt
  io.res := res.asUInt
  io.ress := res
  io.hi  := res(61, 32)
  io.lo  := res(31, 0)
}
/**
  * Experiment
  * Anyone can place codes here for API verification use.
  */
class Experiment extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "Mult test" in {
    test(new ChiselMultTop) { c =>
      c.io.op1.poke("hffffffe0".U)
      c.io.op2.poke(2.U)

      println(c.io.ress.peek())
      println(c.io.res.peek())
      println(c.io.hi.peek())
      println(c.io.lo.peek())
    }
  }

  "Cast test" in {
    test(new CastTestTop) { c=>
      c.io.in.adELd.poke(0.B)
      c.io.in.adELi.poke(1.B)
      c.io.in.adES.poke(0.B)
      c.io.in.bp.poke(1.B)
      c.io.in.ov.poke(0.B)
      c.io.in.ri.poke(1.B)
      c.io.in.sys.poke(1.B)

      c.io.valid.poke(1.B)
      c.io.completed.poke(1.B)
      
      c.io.out.adELd.expect(0.B)
      c.io.out.adELi.expect(1.B)
      c.io.out.adES.expect(0.B)
      c.io.out.bp.expect(1.B)
      c.io.out.ov.expect(0.B)
      c.io.out.ri.expect(1.B)
      c.io.out.sys.expect(1.B)
    }
  }

  "Addr Map test" in {
    test(new AddrMapTestTop) {c =>
      c.io.in.poke("hbfc00000".U)
      println(c.io.out.peek())
    }
  }
  "Paralized bundle test" in {
    test(new ParalizeBundleTester) { c =>
      c.io.in.poke(31.U)

      c.io.out.a1.expect(7.U)
      c.io.out.a2.expect(31.U)
      c.io.out.a3.expect(7.U)
    }
  }

  "Priority encoder test" in {
    test(new EncoderTester) { c =>
      c.io.in.poke("b0000".U)
      c.io.out.expect(3.U)
    }
  }

  "Sign extender test" in {
    test(new SExtTester) { c =>
      c.io.in.poke(0xfffe.U)
      c.io.out.expect(0xfffffffeL.U)
    }
  }

  "MuxCase test" in {
    test(new MuxCaseTester) { c =>
      c.io.out.expect(3.U)
    }
  }

  "Reassgin test" in {
    test(new ReassignTester) { c =>
      c.io.in.a.poke(0.B)
      c.io.in.b.poke(1.B)

      c.io.out.a.expect(1.B)
      c.io.out.b.expect(0.B)
    }
  }

  "Wrap Input test" in {
    test(new InputWrapTest) { c =>
      c.io.out.a.expect(1.B)
      c.io.out.b.expect(1.B)
    }
  }

  
}

class EncoderTester extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(4.W))
    val out = Output(UInt(4.W))
  })

  io.out := PriorityEncoder(io.in)
}

class SExtTester extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(32.W))
    val in  = Input(UInt(16.W))
  })

  io.out := Cat(Fill(16, io.in(15)), io.in(15, 0))
}

class MuxCaseTester extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(5.W))

  })
  io.out := MuxCase(
    0.U,
    Seq(
      0.B -> 1.U,
      0.B -> 2.U,
      1.B -> 3.U,
      1.B -> 4.U,
    )
  )

}

class ReassignTestBundle extends Bundle {
  val a = Bool()
  val b = Bool()
}
class ReassignTester extends Module {
  val io = IO(new Bundle {
    val in  = Input(new ReassignTestBundle)
    val out = Output(new ReassignTestBundle)
  })

  val t = Wire(new ReassignTestBundle)
  t      := io.in
  t.a    := 1.B
  t.b    := 0.B
  io.out := t
}

class ParalizeBundle(longer: Boolean = false) extends Bundle {
  val bundleWidth = if (longer) 5.W else 3.W

  val data = UInt(bundleWidth)
}

class ParalizeBundleTester extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(5.W))
    val out = Output(new Bundle {
      val a1 = UInt(5.W)
      val a2 = UInt(5.W)
      val a3 = UInt(5.W)
    })
  })
  val a1 = Wire(new ParalizeBundle)
  val a2 = Wire(new ParalizeBundle(true))
  val a3 = Wire(new ParalizeBundle(true))

  a1.data := io.in
  a2.data := io.in

  a3 := a1

  io.out.a1 := a1.data
  io.out.a2 := a2.data
  io.out.a3 := a3.data
}

class InputWrapBundle extends Bundle {
  val a = Input(Bool())
  val b = Output(Bool())
}

class InputWrapTest extends Module {
  val io = IO(new Bundle {
    val out = Output(new InputWrapBundle)
  })
  io.out.a := 1.B
  io.out.b := 1.B
}

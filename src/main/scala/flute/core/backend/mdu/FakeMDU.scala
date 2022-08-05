package flute.core.backend.mdu

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class MDUIn extends Bundle {
  val op1    = UInt(dataWidth.W)
  val op2    = UInt(dataWidth.W)
  val signed = Bool()
  val mul    = Bool() // io.md===1.U : Mul
}
// return res = Mux(md, op1*op2, op1/op2) in 4 cycles
class FakeMDU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(DecoupledIO(new MDUIn))
    val res = Output(Valid(new HILORead))

    val flush = Input(Bool())
  })

  val op1    = io.in.bits.op1
  val op2    = io.in.bits.op2
  val mul    = io.in.bits.mul
  val signed = io.in.bits.signed

  val mulRes  = op1.asSInt * op2.asSInt
  val muluRes = op1 * op2

  val mulHi = mulRes(63, 32)
  val mulLo = mulRes(31, 0)

  val muluHi = muluRes(63, 32)
  val muluLo = muluRes(31, 0)

  val divRes = op1.asSInt / op2.asSInt
  val divMod = op1.asSInt % op2.asSInt

  val divuRes = op1 / op2
  val divuMod = op1 % op2

  val hiReg = RegInit(0.U(32.W))
  val loReg = RegInit(0.U(32.W))
  io.res.bits.hi := hiReg
  io.res.bits.lo := loReg

  val idle :: busy :: Nil = Enum(2)
  val state               = RegInit(idle)
  val cnt                 = RegInit(0.U(3.W))

  switch(state) {
    is(idle) {
      when(io.flush) {
        cnt := 0.U
      }.elsewhen(io.in.valid) {
        state := busy
        cnt   := 4.U
        hiReg := MuxCase(
          0.U,
          Seq(
            (mul && signed)   -> mulHi,
            (mul && !signed)  -> muluHi,
            (!mul && signed)  -> divMod.asUInt,
            (!mul && !signed) -> divuMod
          )
        )
        loReg := MuxCase(
          0.U,
          Seq(
            (mul && signed)   -> mulLo,
            (mul && !signed)  -> muluLo,
            (!mul && signed)  -> divRes.asUInt,
            (!mul && !signed) -> divuRes
          )
        )
      }
    }
    is(busy) {
      when(io.flush) {
        state := idle
        cnt   := 0.U
      }.elsewhen(cnt === 1.U) {
        state := idle
        cnt   := 0.U
      }.otherwise {
        cnt := cnt - 1.U
      }
    }
  }

  io.in.ready  := state === idle
  io.res.valid := cnt === 1.U
}

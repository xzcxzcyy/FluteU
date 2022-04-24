package flute.core.components

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._
import flute.core.decode.StoreMode

class SbufferEntry extends Bundle {
  val addr  = UInt(30.W)
  val data  = UInt(32.W)
  val valid = Vec(4, Bool())
}

class SbufferWrite(entryAmount: Int) extends Bundle {
  val robAddr   = Input(UInt(log2Up(entryAmount).W))
  val memAddr   = Input(UInt(32.W))
  val memData   = Input(UInt(32.W))
  val storeMode = Input(UInt(StoreMode.width.W))
  val valid     = Input(Bool())
}

/**
  * 读接口总是读四个字节，但这四个字节未必全部有效；读接口只希望接收地址的高30位。
  *
  * @param entryAmount
  */
class SbufferRead(entryAmount: Int) extends Bundle {
  val memGroupAddr = Input(UInt(30.W))
  val data         = Output(Vec(4, UInt(8.W)))
  val valid        = Output(Vec(4, Bool()))
}

/**
  * Sbuffer: 以 robAddr 为索引，每一项是当前指令希望向存储器的哪些字节写入哪些信息。
  *          保证对同一字节最多只有一个 valid
  * 
  *          注意：读接口只希望接收地址的高30位，而不是全部的地址
  *
  * @param entryAmount Store Buffer 的大小
  */
class Sbuffer(entryAmount: Int) extends Module {

  val io = IO(new Bundle {
    val write  = new SbufferWrite(entryAmount)
    val read   = new SbufferRead(entryAmount)
    val retire = Flipped(ValidIO(UInt(log2Up(entryAmount).W)))
    val flush  = Input(Bool())
  })

  val entries = RegInit(VecInit(Seq.fill(entryAmount)(0.U.asTypeOf(new SbufferEntry))))

  val writeGroupAddr = io.write.memAddr(31, 2)
  val writeOffset    = io.write.memAddr(1, 0)
  val writeSbEntry   = WireInit(0.U.asTypeOf(new SbufferEntry))

  writeSbEntry.addr := writeGroupAddr
  writeSbEntry.data := io.write.memData
  when(io.write.storeMode === StoreMode.word) {
    writeSbEntry.valid := VecInit("b1111".U(4.W).asBools)
  }.elsewhen(io.write.storeMode === StoreMode.halfword) {
    when(writeOffset(1)) {
      writeSbEntry.valid := VecInit("b1100".U(4.W).asBools)
    }.otherwise {
      writeSbEntry.valid := VecInit("b0011".U(4.W).asBools)
    }
  }.elsewhen(io.write.storeMode === StoreMode.byte) {
    writeSbEntry.valid(writeOffset) := 1.B
  }

  when(io.write.valid && !io.flush) {
    entries(io.write.robAddr) := writeSbEntry
  }
  for (i <- 0 until entryAmount) {
    for (j <- 0 until 4) {
      val deactive = io.write.valid &&
        io.write.robAddr =/= i.U &&
        writeGroupAddr === entries(i).addr &&
        writeSbEntry.valid(j) &&
        entries(i).valid(j)
      when(deactive) {
        entries(i).valid(j) := 0.B
      }
    }
  }

  val readGroupAddr = io.read.memGroupAddr
  val sbReadData    = WireInit(VecInit(Seq.fill(4)(0.U(8.W))))
  val sbReadValid   = WireInit(VecInit(Seq.fill(4)(0.B)))
  for (i <- 0 until entryAmount) {
    for (j <- 0 until 4) {
      val valid = entries(i).valid(j) && entries(i).addr === readGroupAddr
      when(valid) {
        sbReadData(j)  := entries(i).data(j * 8 + 7, j * 8)
        sbReadValid(j) := 1.B
      }
    }
  }
  io.read.valid := sbReadValid
  io.read.data  := sbReadData

  when(io.retire.valid) {
    entries(io.retire.bits).valid := VecInit("b0000".U(4.W).asBools)
  }

  when(io.flush) {
    entries.foreach(_.valid := VecInit("b0000".U(4.W).asBools))
  }
}

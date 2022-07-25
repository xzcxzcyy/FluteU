package flute.core.components

import chisel3._
import chisel3.util._

import flute.config.CPUConfig._
import flute.core.backend.decode.StoreMode

class SbufferEntry extends Bundle {
  val addr  = UInt(30.W)
  val data  = UInt(32.W)
  val valid = Vec(4, Bool())
}

class SbufferWrite extends Bundle {
  val memAddr   = Input(UInt(32.W))
  val memData   = Input(UInt(32.W))
  val storeMode = Input(UInt(StoreMode.width.W))
  val valid     = Input(Bool())
  val ready     = Output(Bool())
}

/**
  * 读接口总是读四个字节，但这四个字节未必全部有效；读接口只希望接收地址的高30位。
  *
  * @param entryAmount
  */
class SbufferRead extends Bundle {
  val memGroupAddr = Input(UInt(30.W))
  val data         = Output(UInt(32.W))
  val valid        = Output(Vec(4, Bool()))
}

/**
  * Sbuffer: 每一项是当前指令希望向存储器的哪些字节写入哪些信息。
  *          Notes: 不保证对同一字节最多只有一个 valid，请求查询逻辑额外处理。
  * 
  *          注意：读接口只希望接收地址的高30位，而不是全部的地址
  *
  * @param entryAmount Store Buffer 的大小
  */
class Sbuffer extends Module {
  assert(isPow2(sbufferAmount) && sbufferAmount > 1)
  private val sbPtrWidth = log2Up(sbufferAmount)
  val io = IO(new Bundle {
    val write  = new SbufferWrite
    val read   = new SbufferRead
    val retire = Input(Bool())
    val flush  = Input(Bool())
  })
  val entries = RegInit(VecInit(Seq.fill(sbufferAmount)(0.U.asTypeOf(new SbufferEntry))))
  // [headPtr, tailPtr)
  val headPtr   = RegInit(0.U(sbPtrWidth.W))
  val tailPtr   = RegInit(0.U(sbPtrWidth.W))
  val curNumber = tailPtr - headPtr
  val hasRoom   = curNumber < (sbufferAmount - 1).U
  val writeFire = hasRoom && io.write.valid

  val flush      = io.flush
  val writeEntry = SbUtils.writePort2Entry(WireInit(io.write))
  val nextHeadPtr = (headPtr + 1.U)(sbPtrWidth - 1, 0)

  io.write.ready := hasRoom

  when(writeFire && !flush) {
    entries(tailPtr) := writeEntry
    tailPtr          := tailPtr + 1.U
  }

  when(io.retire && !flush && curNumber > 0.U) {
    headPtr := nextHeadPtr
  }

  when(flush) {
    headPtr := 0.U
    tailPtr := 0.U
  }

  val (readData, readValid) = SbUtils.readResult(
    io.read.memGroupAddr,
    entries,
    headPtr,
    tailPtr
  )
  io.read.data := readData
  io.read.valid := readValid
}

object SbUtils {
  def writePort2Entry(port: SbufferWrite): SbufferEntry = {
    val ret      = WireInit(0.U.asTypeOf(new SbufferEntry))
    val byteBias = port.memAddr(1, 0)
    val halfBias = byteBias(1)

    ret.addr := port.memAddr(31, 2)
    switch(port.storeMode) {
      is(StoreMode.word) {
        ret.valid := VecInit("b1111".U(4.W).asBools)
        ret.data  := port.memData
      }
      is(StoreMode.halfword) {
        ret.valid := Mux(
          halfBias,
          VecInit("b1100".U(4.W).asBools),
          VecInit("b0011".U(4.W).asBools),
        )
        ret.data := Mux(
          halfBias,
          Cat(port.memData(15, 0), 0.U(16.W)),
          Cat(0.U(16.W), port.memData(15, 0)),
        )
      }
      is(StoreMode.byte) {
        ret.valid(byteBias) := 1.B
        ret.data := MuxLookup(
          key = byteBias,
          default = 0.U,
          mapping = Seq(
            0.U -> Cat(0.U(24.W), port.memData(7, 0)),
            1.U -> Cat(0.U(16.W), port.memData(7, 0), 0.U(8.W)),
            2.U -> Cat(0.U(8.W), port.memData(7, 0), 0.U(16.W)),
            3.U -> Cat(port.memData(7, 0), 0.U(24.W))
          )
        )
      }
    }
    ret
  }

  def reorder(
      entries: Vec[SbufferEntry],
      head: UInt,
      tail: UInt,
  ): Vec[SbufferEntry] = {
    val sbPtrWidth = log2Up(sbufferAmount)
    assert(head.getWidth == sbPtrWidth)
    assert(tail.getWidth == sbPtrWidth)

    val result    = WireInit(VecInit(Seq.fill(sbufferAmount)(0.U.asTypeOf(new SbufferEntry))))
    val curNumber = tail - head

    for (i <- 0 until sbufferAmount) {
      when(i.U < curNumber) {
        result(i) := entries((i.U + head)(sbPtrWidth - 1, 0))
      }
    }

    result
  }

  def readResult(
      memGroupAddr: UInt,
      entries: Vec[SbufferEntry],
      head: UInt,
      tail: UInt,
  ): (UInt, Vec[Bool]) = {
    val data  = WireInit(VecInit(Seq.fill(4)(0.U(8.W))))
    val valid = WireInit(VecInit("b0000".U(4.W).asBools))

    val reOrdered = reorder(entries, head, tail)
    for (i <- 0 until sbufferAmount) {
      for (byte <- 0 until 4) {
        when(reOrdered(i).addr === memGroupAddr && reOrdered(i).valid(byte)) {
          valid(byte) := 1.B
          data(byte)  := reOrdered(i).data(8 * byte + 7, 8 * byte)
        }
      }
    }

    (Cat(data(3), data(2), data(1), data(0)), valid)
  }
}

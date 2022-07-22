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

/**
  * TODO: fix and migrate to decoupled.
  *
  */
class SbufferWrite extends Bundle {
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
class SbufferRead extends Bundle {
  val memGroupAddr = Input(UInt(30.W))
  val data         = Output(UInt(32.W))
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
class Sbuffer extends Module {
  assert(isPow2(sbufferAmount) && sbufferAmount > 1)
  val io = IO(new Bundle {
    val write  = new SbufferWrite
    val read   = new SbufferRead
    val retire = Input(Bool())
    val flush  = Input(Bool())
  })
  val entries = RegInit(VecInit(Seq.fill(sbufferAmount)(0.U.asTypeOf(new SbufferEntry))))
}

object SbUtils {}

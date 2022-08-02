package flute.util

import chisel3._
import chisel3.util._

object AddrMap {
  def map(addr: UInt) = {
    assert(addr.getWidth == 32)
    val res  = Wire(UInt(32.W))
    res := addr
    when(addr >= "ha0000000".U && addr < "hc0000000".U) {
      res := addr - "ha0000000".U
    }.elsewhen(addr >= "h80000000".U && addr < "ha0000000".U) {
      res := addr - "h80000000".U
    }

    res
  }
}

object Test extends App {
  println(AddrMap.map("hbfc00000".U))
}
package flute.cache

import chisel3._
import chisel3.util.MuxLookup
import flute.config.CPUConfig._
import flute.core.decode.StoreMode
import chisel3.util.experimental.loadMemoryFromFileInline
import dataclass.data


class DCacheIO extends Bundle {
  val addr  = Input(UInt(addrWidth.W))
  val readData  = Output(UInt(dataWidth.W))
  val storeMode = Input(UInt(StoreMode.width.W))
  val writeData = Input(UInt(dataWidth.W))
}
// little-endian
class DCache(memoryFile: String = "test_data/dmem.in") extends Module {
  val io = IO(new Bundle {
    val port = Vec(superscalar, new DCacheIO())
  })

  val mem = Mem(1024, UInt(dataWidth.W))

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  for(i <- 0 until superscalar){
    val pio = io.port(i)

    pio.readData := mem(pio.addr(31,2))

    when(pio.storeMode === StoreMode.byte){
      mem(pio.addr(31,2)) := pio.writeData(7,0)
    }
    when(pio.storeMode === StoreMode.halfword){
      mem(pio.addr(31,2)) := pio.writeData(13,0)
    }
    when(pio.storeMode === StoreMode.word){
      mem(pio.addr(31,2)) := pio.writeData
    }

  }
}

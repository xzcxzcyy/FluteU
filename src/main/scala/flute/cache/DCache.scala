package flute.cache

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.backend.decode.StoreMode
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

  val mem = Mem(32 * 4, UInt(byteWidth.W))

  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
  }

  for(i <- 0 until superscalar){
    val pio = io.port(i)

    pio.readData := Cat(mem(pio.addr+3.U),mem(pio.addr+2.U),mem(pio.addr+1.U),mem(pio.addr))

    when(pio.storeMode === StoreMode.byte){
      mem(pio.addr) := pio.writeData(7,0)
    }
    when(pio.storeMode === StoreMode.halfword){
      for(i<- 0 to 1){mem(pio.addr+i.U) := pio.writeData(8*(i+1)-1,8*i) }
    }
    when(pio.storeMode === StoreMode.word){
      for(i<- 0 to 3){mem(pio.addr+i.U) := pio.writeData(8*(i+1)-1,8*i) }
    }

  }
}

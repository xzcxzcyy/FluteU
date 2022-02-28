package cache

import chisel3._
import config.CPUConfig._
import core.decode.StoreMode

// no statemachine, no axi, no
/**
  * Addresses MUST be aligned to 4 bytes
  * 
  * @param setAmount how many sets
  * @param wayAmount how many ways
  * @param bankAmount how many banks
  */
class Cache(
    setAmount: Int  = 64,
    wayAmount: Int  = 4,
    bankAmount: Int = 16
) extends Module {
    val io = IO(new Bundle {
        val readAddr  = Input(UInt(addrWidth.W))
        val writeAddr = Input(UInt(addrWidth.W))
        val storeMode = Input(UInt(StoreMode.width.W))
        val readData  = Output(UInt(dataWidth.W))
        val writeData = Input(UInt(dataWidth.W))
    })
/**
    * |   tag     |   index     |   bankOffset      | 0.U(2.W)  |
    * | `tagLen`  | `indexLen`  | `log2(bankAmount)`|     2     |
    */

    /*
    val indexLen = log2Ceil(setAmount)
    val blockSize = bankAmount * dataWidth / 8
    val tagLen = addrWidth - indexLen - log2Ceil(blockSize)

    val readTag =  

    assert(content.length > 0)
    val hardContent = content.map(e => e.U(dataWidth.W))
    val ram         = RegInit(VecInit(hardContent))

    io.readData := ram(io.readAddr(31, 2))
    when (io.storeMode =/= StoreMode.disable) {
        ram(io.writeAddr(31, 2)) := io.writeData
    }
    */
}

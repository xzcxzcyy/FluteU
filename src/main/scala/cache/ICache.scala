package flute.cache

import flute.config._

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.DecoupledIO

class ICacheIO(implicit conf: CPUConfig) extends Bundle {
  val addr = Flipped(new DecoupledIO(UInt(conf.addrWidth.W)))
  val data = new DecoupledIO(Vec(conf.fetchGroupSize, UInt(conf.dataWidth.W)))
}

class ICache(implicit conf: CPUConfig) extends Module {
  val io = IO(new ICacheIO)

  val memAddr = io.addr.bits(31, 2)
  
  val mem  = SyncReadMem(1024, UInt(conf.dataWidth.W))
  val addrBuf = RegInit(0.U(conf.addrWidth.W))
  val dataBuf = RegInit(VecInit(Seq.fill(conf.fetchGroupSize)(0.U(conf.instrWidth.W))))
  val state = RegInit(0.U(2.W))

  val memPorts = for (i <- 0 to conf.fetchGroupSize - 1) yield mem.read(memAddr + i.U)

  when (state === 0.U) {
    when (io.addr.valid) {
      state := 1.U
      addrBuf := io.addr.bits
    } .otherwise {
      state := 0.U
    }
  } .elsewhen(state === 1.U) {
    for (i <- 0 to conf.fetchGroupSize - 1) yield dataBuf(i.U) := memPorts(i)
    when (io.addr.valid) {
      state := 2.U
      addrBuf := io.addr.bits
    } .otherwise {
      state := 3.U
    }
  } .elsewhen(state === 2.U) {
    for (i <- 0 to conf.fetchGroupSize - 1) yield dataBuf(i.U) := memPorts(i)
    when (io.addr.valid) {
      state := 2.U
      addrBuf := io.addr.bits
    } .otherwise {
      state := 3.U
    }
  } .otherwise {
    when (io.addr.valid) {
      state := 1.U
      addrBuf := io.addr.bits
    } .otherwise {
      state := 0.U
    }

  }

  io.data.valid := (state === 2.U || state === 3.U)
  io.addr.ready := 1.B
  io.data.bits := dataBuf
}

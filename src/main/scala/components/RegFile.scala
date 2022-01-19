package components

import chisel3._
import config.CpuConfig._

class RegFile extends Module {
  val io = IO(new RegFileIO)
  val regfile = RegInit(VecInit(Seq.fill(regAmount)(0.U(dataWidth.W))))
  io.r1Data := regfile(io.r1Addr)
  io.r2Data := regfile(io.r2Addr)
  when (io.writeEnable & (io.writeAddr =/= 0.U(regAddrWidth.W))) {
    regfile(io.writeAddr) := io.writeData
  }
}

class RegFileIO extends Bundle {
  val r1Addr = Input(UInt(regAddrWidth.W))
  val r2Addr = Input(UInt(regAddrWidth.W))
  val writeAddr = Input(UInt(regAddrWidth.W))
  val writeData = Input(UInt(dataWidth.W))
  val writeEnable = Input(Bool())
  ///
  val r1Data = Output(UInt(dataWidth.W))
  val r2Data = Output(UInt(dataWidth.W))
}

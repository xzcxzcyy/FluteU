package components

import chisel3._

class RegFile extends Module {
  val io = IO(new RegFileIO)
  val regfile = RegInit(VecInit(Seq.fill(config.numOfReg)(0.U(config.regDataWidth))))
  io.r1Data := regfile(io.r1Addr)
  io.r2Data := regfile(io.r2Addr)
  when (io.writeEnable & (io.writeAddr =/= 0.U(config.regAddrWidth))) {
    regfile(io.writeAddr) := io.writeData
  }
}

private object config {
  val regAddrWidth = 5.W
  val regDataWidth = 32.W
  val numOfReg = 32
}

class RegFileIO extends Bundle {
  val r1Addr = Input(UInt(config.regAddrWidth))
  val r2Addr = Input(UInt(config.regAddrWidth))
  val writeAddr = Input(UInt(config.regAddrWidth))
  val writeData = Input(UInt(config.regDataWidth))
  val writeEnable = Input(Bool())
  ///
  val r1Data = Output(UInt(config.regDataWidth))
  val r2Data = Output(UInt(config.regDataWidth))
}

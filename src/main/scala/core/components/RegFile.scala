package core.components

import chisel3._
import config.CPUConfig._

class RegFile(numRead: Int, numWrite: Int) extends Module {
  val io      = IO(new RegFileIO(numRead, numWrite))
  val regfile = RegInit(VecInit(Seq.fill(regAmount)(0.U(dataWidth.W))))
  for (i <- 0 to numRead) {
    io.read(i).r1Data := regfile(io.read(i).r1Addr)
    io.read(i).r2Data := regfile(io.read(i).r2Addr)
  }
  for (i <- 0 to numWrite) {
    when(io.write(i).writeEnable && (io.write(i).writeAddr =/= 0.U(regAddrWidth.W))) {
      regfile(io.write(i).writeAddr) := io.write(i).writeData
    }
  }
  io.debug := regfile
}

class RegFileIO(numRead: Int, numWrite: Int) extends Bundle {
  val read = Vec(numRead, new RegFileReadIO)
  ///
  val write = Vec(numWrite, new RegFileWriteIO)
  ///
  val debug = Output(Vec(regAmount, UInt(dataWidth.W)))
}

class RegFileWriteIO extends Bundle {
  val writeAddr   = Input(UInt(regAddrWidth.W))
  val writeData   = Input(UInt(dataWidth.W))
  val writeEnable = Input(Bool())
}

class RegFileReadIO extends Bundle {
  val r1Addr = Input(UInt(regAddrWidth.W))
  val r2Addr = Input(UInt(regAddrWidth.W))
  val r1Data = Output(UInt(dataWidth.W))
  val r2Data = Output(UInt(dataWidth.W))
}

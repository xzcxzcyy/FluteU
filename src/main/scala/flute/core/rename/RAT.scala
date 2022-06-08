package flute.core.rename

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class RATReadPort extends Bundle {
  val ready = Input(Bool())
  val addr = Input(UInt(archRegAddrWidth.W))
  val data = Output(UInt(phyRegAddrWidth.W))
  val valid = Output(Bool())
}

class RATWritePort extends Bundle {
  val wen = Input(Bool())
  val addr = Input(UInt(archRegAddrWidth.W))
  val data = Input(UInt(phyRegAddrWidth.W))
}

class RATBundle extends Bundle {
  val ReadPorts = Vec(superscalar,Vec(3,new RATReadPort))
  val WritePorts = Vec(superscalar,new RATWritePort)
}

// TODO: when encountering branch instruction, rat should be backuped as checkpoint

class RAT extends Module{
  val io = IO(new RATBundle)
  // read first: Synchronic Read, next clock write takes effect.
  val srat = Mem(archRegAmount,UInt(phyRegAddrWidth.W))
  //init
  for(i<-0 until superscalar; j <- 0 until 3){io.ReadPorts(i)(j).data := DontCare}
  
  for(i <- 0 until superscalar; j <- 0 until 3){
    when(io.ReadPorts(i)(j).ready){
      io.ReadPorts(i)(j).data := srat(io.ReadPorts(i)(j).addr)
      io.ReadPorts(i)(j).valid := 1.B
    }
  }

  for (w <- io.WritePorts) {
    when (w.wen) {
      srat(w.addr) := w.data
    }
  }
}
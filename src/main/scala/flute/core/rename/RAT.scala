package flute.core.rename

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class RATReadPort extends Bundle {
  val addr = Input(UInt(LogicRegIdxWidth.W))
  val data = Output(UInt(PhyRegIdxWidth.W))
}

class RATWritePort extends Bundle {
  val wen = Input(Bool())
  val addr = Input(UInt(LogicRegIdxWidth.W))
  val data = Input(UInt(PhyRegIdxWidth.W))
}

class RATBundle extends Bundle {
  val ReadPorts = Vec(superscalar,Vec(3,DecoupledIO(new RATReadPort)))
  val WritePorts = Vec(superscalar,new RATWritePort)
}

// TODO: when encountering branch instruction, rat should be backuped as checkpoint

class RAT extends Module{
  val io = IO(new RATBundle)
  // read first: Synchronic Read, next clock write takes effect.
  val srat = Mem(LogicRegAmount,UInt(PhyRegIdxWidth.W))

  for(i<-0 until superscalar; j <- 0 until 3){io.ReadPorts(i)(j).bits.data := DontCare}
  
  for(i <- 0 until superscalar; j <- 0 until 3){
    when(io.ReadPorts(i)(j).ready){
      io.ReadPorts(i)(j).bits.data := srat(io.ReadPorts(i)(j).bits.addr)
      io.ReadPorts(i)(j).valid := 1.B
    }
  }

  for (w <- io.WritePorts) {
    when (w.wen) {
      srat(w.addr) := w.data
    }
  }
}

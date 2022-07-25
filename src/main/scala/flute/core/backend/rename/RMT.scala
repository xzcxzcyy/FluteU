package flute.core.backend.rename

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class RMTReadPort extends Bundle {
  val addr = Input(UInt(archRegAddrWidth.W))
  val data = Output(UInt(phyRegAddrWidth.W))
}

class RMTWritePort extends Bundle {
  val en   = Input(Bool())
  val addr = Input(UInt(archRegAddrWidth.W))
  val data = Input(UInt(phyRegAddrWidth.W))
}

class RMTCommit(numCommit: Int) extends Bundle {
  val write = Vec(numCommit, new RMTWritePort)
}

class RMTDebugOut extends Bundle {
  val sRAT = Output(Vec(archRegAmount, UInt(phyRegAddrWidth.W)))
  val aRAT = Output(Vec(archRegAmount, UInt(phyRegAddrWidth.W)))
}

// Map: arch -> phy
class RMT(numWays: Int, numCommit: Int, release: Boolean = false) extends Module {
  val io = IO(new Bundle {
    // 每条指令需要三个读端口 0->dest;1->src1;2->src2
    val read = Vec(numWays, Vec(3, new RMTReadPort))

    // 每条指令需要一个写端口
    val write = Vec(numWays, new RMTWritePort)

    // commit to aRAT
    val commit = new RMTCommit(numCommit)

    val chToArch = Input(Bool())

    val debug = if (!release) Some(new RMTDebugOut) else None
  })

  // reset init all arch reg map to phy reg $0
  val sRAT = RegInit(VecInit(Seq.fill(archRegAmount)(0.U(phyRegAddrWidth.W))))
  val aRAT = RegInit(VecInit(Seq.fill(archRegAmount)(0.U(phyRegAddrWidth.W))))

  // sRAT
  for (i <- 0 until numWays; tp <- 0 until 3) {
    val archRegAddr = io.read(i)(tp).addr
    val phyRegAddr  = sRAT(archRegAddr)
    io.read(i)(tp).data := phyRegAddr
  }

  for (i <- 0 until numWays) {
    val en          = io.write(i).en
    val archRegAddr = io.write(i).addr
    val phyRegAddr  = io.write(i).data
    when(en && !io.chToArch && archRegAddr =/= 0.U) {
      sRAT(archRegAddr) := phyRegAddr
    }
  }

  // aRAT
  val nextARat = WireInit(aRAT)
  for (i <- 0 until numCommit) {
    val en          = io.commit.write(i).en
    val archRegAddr = io.commit.write(i).addr
    val phyRegAddr  = io.commit.write(i).data
    when(en && archRegAddr =/= 0.U) {
      nextARat(archRegAddr) := phyRegAddr
    }
  }
  aRAT := nextARat

  // aRAT -> sRAT
  when(io.chToArch) {
    sRAT := nextARat
  }

  if (!release) {
    io.debug.get.aRAT := aRAT
    io.debug.get.sRAT := sRAT
  }

}

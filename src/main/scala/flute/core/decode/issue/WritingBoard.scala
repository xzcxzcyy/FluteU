package flute.core.decode.issue

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class QueryWBIO extends Bundle {
  val addrIn = Vec(WBconfig.queryPort, Input(UInt(regAddrWidth.W)))
  val dataOut = Vec(WBconfig.queryPort, Output(UInt(WBconfig.width.W)))
}

object WBconfig {
  val width = 3
  val queryPort = 4
}

class WritingBoard extends Module {

  val io = IO(new Bundle {
    val checkIn  = Vec(2, Flipped(ValidIO(UInt(regAddrWidth.W)))) // reg to plus 1
    val checkOut = Vec(2, Flipped(ValidIO(UInt(regAddrWidth.W)))) // reg to minus 1

    val query = new QueryWBIO
  })

  val regs = RegInit(VecInit(Seq.fill(regAmount)(0.U(WBconfig.width.W))))

  // band addr
  for (i <- 0 until WBconfig.queryPort) io.query.dataOut(i) := regs(io.query.addrIn(i))

  for (i <- 0 until regAmount) {
    // check in
    val checkInSeq = for (j <- 0 until 2) yield io.checkIn(j).valid && (i.U === io.checkIn(j).bits)
    val checkInNum = PopCount(checkInSeq)

    // check out
    val checkOutSeq = for (j <- 0 until 2) yield io.checkOut(j).valid && (i.U === io.checkOut(j).bits)
    val checkOutNum = PopCount(checkOutSeq)

    regs(i.U) := regs(i.U) + checkInNum - checkOutNum
  }
}

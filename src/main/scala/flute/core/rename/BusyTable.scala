package flute.core.rename

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._

class BusyTableReadPort extends Bundle {
  val addr = Input(UInt(phyRegAddrWidth.W))
  val busy = Output(Bool()) // busy = 1.B means that phyReg is in pipeline computing
}

class BusyTable(nRead: Int, nCheckIn: Int, nCheckOut: Int) extends Module {
  private val n = phyRegAmount

  val io = IO(new Bundle {
    val read = Vec(nRead, new BusyTableReadPort)

    val checkIn  = Input(Vec(nCheckIn, Valid(UInt(phyRegAddrWidth.W))))
    val checkOut = Input(Vec(nCheckOut, Valid(UInt(phyRegAddrWidth.W))))

    val debug = new Bundle {
      val table = Output(UInt(phyRegAmount.W))
    }
  })

  val busyTable = RegInit(VecInit(Seq.fill(phyRegAmount)(0.B)))

  // for (i <- 0 until nRead) {
  //   io.read(i).busy := busyTable(io.read(i).addr)
  // }
  
  for (i <- 0 until nRead) {
    val busy = (UIntToOH(io.read(i).addr)(n - 1, 0) & busyTable.asUInt).orR
    io.read(i).busy := busy
  }

  val checkIMask = io.checkIn.map(d => UIntToOH(d.bits)(n - 1, 0) & Fill(n, d.valid)).reduce(_ | _)
  val checkOMask = io.checkOut.map(d => UIntToOH(d.bits)(n - 1, 0) & Fill(n, d.valid)).reduce(_ | _)

  val nextTable = (busyTable.asUInt | checkIMask) & ~checkOMask // & 优先级大于 |

  busyTable := nextTable.asBools

  // different method 

  // for (i <- 0 until nCheckIn) {
  //   when(io.checkIn(i).valid) {
  //     busyTable(io.checkIn(i).bits) := 1.B
  //   }
  // }

  // for (i <- 0 until nCheckOut) {
  //   when(io.checkOut(i).valid) {
  //     busyTable(io.checkOut(i).bits) := 0.B
  //   }
  // }

  io.debug.table := busyTable.asUInt
}

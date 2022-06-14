package flute.core.rename

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp
import flute.config.CPUConfig._

class Rename_new(nWays: Int, nCommit: Int) extends Module {
  val io = IO(new Bundle {
    val decode = Vec(nWays, Flipped(ValidIO(new MicroOp)))
    // commit

    // TODO ROB check for stall
    val stall    = Input(Bool())
    val stallReq = Output(Bool())
  })

  val freelist = Module(new Freelist(nWays, phyRegAmount))
  val rat      = Module(new RMT(nWays, nCommit))

  class RenameEntry extends Bundle {
    val srcL      = UInt(phyRegAddrWidth.W)
    val srcR      = UInt(phyRegAddrWidth.W)
    val originReg = UInt(phyRegAddrWidth.W)
    val writeReg  = UInt(phyRegAddrWidth.W)
    val writeEn   = Bool()
  }

  val ideal = Wire(Vec(nWays, new RenameEntry))
  val real  = Wire(Vec(nWays, new RenameEntry))
  val uops  = Wire(Vec(nWays, new MicroOp))

  uops := io.decode.map(d => d.bits) // raw microOp before renaming

  // ideal way
  for (i <- 0 until nWays) {
    // RAT read
    rat.io.read(i)(0).addr := io.decode(i).bits.writeRegAddr
    rat.io.read(i)(1).addr := io.decode(i).bits.rsAddr
    rat.io.read(i)(2).addr := io.decode(i).bits.rtAddr

    ideal(i).srcL      := rat.io.read(i)(1).data
    ideal(i).srcR      := rat.io.read(i)(2).data
    ideal(i).originReg := rat.io.read(i)(0).data
    ideal(i).writeReg  := freelist.io.allocPregs(i).bits
    ideal(i).writeEn   := io.decode(i).bits.regWriteEn
  }

  // RAW Check
  real(0).srcL := ideal(0).srcL
  real(0).srcR := ideal(0).srcR

  for (i <- 1 until nWays) {
    val fireL    = Wire(Vec(i, Bool()))
    val fireR    = Wire(Vec(i, Bool()))
    val writeReg = Wire(Vec(i, UInt(phyRegAddrWidth.W)))
    for (j <- 0 until i) {
      fireL(j)    := ideal(j).writeEn && ideal(j).writeReg === ideal(i).srcL
      fireR(j)    := ideal(j).writeEn && ideal(j).writeReg === ideal(i).srcR
      writeReg(j) := ideal(j).writeReg
    }
    // 注意倒序 match case
    real(i).srcL := MuxCase(
      ideal(i).srcL,
      for (j <- i - 1 to 0 by -1) yield { fireL(j) -> writeReg(j) }
    )

    real(i).srcR := MuxCase(
      ideal(i).srcR,
      (i - 1 to 0 by -1).map(j => (fireR(j) -> writeReg(j)))
    ) // same mean diffrent expression
  }

  // WAW Check

  // RAT Write Check
  val wRATen = Wire(Vec(nWays, Bool()))
  for (i <- 0 until nWays) {
    var writeEn = uops(i).regWriteEn
    for (j <- i + 1 until nWays) {
      val legal = !(uops(j).regWriteEn && (uops(j).writeRegAddr === uops(i).writeRegAddr))
      writeEn = writeEn && legal
    }
    wRATen(i) := writeEn
  }

  // Origin Reg Check
  real(0).originReg := ideal(0).originReg
  for (i <- 1 until nWays) {
    val fire      = Wire(Vec(i, Bool()))
    val originReg = Wire(Vec(i, UInt(phyRegAddrWidth.W)))
    for (j <- 0 until i) { //  0 <= j < i
      fire(j) := uops(j).regWriteEn && uops(j).writeRegAddr === uops(i).writeRegAddr
      // no need to && uop(i).regWriteEn, think why :)
      originReg(j) := uops(j).writeRegAddr
    }

    real(i).originReg := MuxCase(
      ideal(i).originReg,
      for (j <- i - 1 to 0 by -1) yield { fire(j) -> originReg(j) }
    )
  }

  for (i <- 0 until nWays) {
    real(i).writeReg := ideal(i).writeReg
    real(i).writeEn  := ideal(i).writeEn
  }

  // 重要的信号: freelist.io.requests, rat.io.write.en 均会改变机器状态（推测态）

  // check if enough to allocate

  val needStall = Wire(Vec(nWays, Bool()))
  for (i <- 0 until nWays) {
    needStall(i) := uops(i).regWriteEn && !freelist.io.allocPregs(i).valid
  }
  val stallReq = needStall.reduce(_ | _)

  for (i <- 0 until nWays) {
    // 当且仅当 来源数据有效 且 能够分配一组资源 且 外部无暂停信号，正式进行请求
    val valid = io.decode(i).valid && !stallReq && !io.stall

    freelist.io.requests(i) := valid && uops(i).regWriteEn
    rat.io.write(i).en      := valid && wRATen(i)

    rat.io.write(i).addr := uops(i).writeRegAddr
    rat.io.write(i).data := real(i).writeReg
  }

  io.stallReq := stallReq

}

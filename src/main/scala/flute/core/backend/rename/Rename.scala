package flute.core.backend.rename

import chisel3._
import chisel3.util._
import flute.core.backend.decode.MicroOp
import flute.config.CPUConfig._
import flute.core.backend.commit._
import flute.core.backend.decode.BJCond

class RenameEntry extends Bundle {
  val srcL      = UInt(phyRegAddrWidth.W)
  val srcR      = UInt(phyRegAddrWidth.W)
  val originReg = UInt(phyRegAddrWidth.W)
  val writeReg  = UInt(phyRegAddrWidth.W)
}

class RenameCommit(nCommit: Int) extends Bundle {
  val freelist = new FreelistCommit(nCommit)
  val rmt      = new RMTCommit(nCommit)
  val chToArch = Input(Bool())
}

class Rename(nWays: Int, nCommit: Int) extends Module {
  val io = IO(new Bundle {
    val decode   = Vec(nWays, Flipped(ValidIO(new MicroOp)))
    val dispatch = Vec(nWays, ValidIO(new MicroOp(rename = true)))
    val rob      = Vec(nWays, Flipped(new ROBWrite(robEntryAmount)))

    // commit
    val commit = new RenameCommit(nCommit)

    // to busyTable
    val checkIn = Output(Vec(nWays, Valid(UInt(phyRegAddrWidth.W))))

    val stall    = Input(Bool())
    val stallReq = Output(Bool())

    val rmtDebug = new RMTDebugOut
  })

  val freelist = Module(new Freelist(nWays, nCommit))
  val rat      = Module(new RMT(nWays, nCommit))

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
  }

  // RAW Check
  real(0).srcL := ideal(0).srcL
  real(0).srcR := ideal(0).srcR

  for (i <- 1 until nWays) {
    val fireL    = Wire(Vec(i, Bool()))
    val fireR    = Wire(Vec(i, Bool()))
    val writeReg = Wire(Vec(i, UInt(phyRegAddrWidth.W)))
    for (j <- 0 until i) {
      fireL(j)    := uops(j).regWriteEn && uops(j).writeRegAddr === uops(i).rsAddr
      fireR(j)    := uops(j).regWriteEn && uops(j).writeRegAddr === uops(i).rtAddr
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
    val fire    = Wire(Vec(i, Bool()))
    val phyWReg = Wire(Vec(i, UInt(phyRegAddrWidth.W)))
    for (j <- 0 until i) { //  0 <= j < i
      fire(j) := uops(j).regWriteEn && uops(j).writeRegAddr === uops(i).writeRegAddr
      // no need to && uop(i).regWriteEn, think why :)
      phyWReg(j) := real(j).writeReg
    }

    real(i).originReg := MuxCase(
      ideal(i).originReg,
      for (j <- i - 1 to 0 by -1) yield { fire(j) -> phyWReg(j) }
    )
  }

  for (i <- 0 until nWays) {
    real(i).writeReg := ideal(i).writeReg
  }

  // 重要的信号: freelist.io.requests, rat.io.write.en 均会改变机器状态（推测态; io.stallReq 会影响流水其他部分
  // 因此这些信号的生成要注意正确结合流水线上下行的 valid/ready 信号

  // check if enough to allocate

  val freeStall = Wire(Vec(nWays, Bool()))
  val robStall  = Wire(Vec(nWays, Bool()))
  for (i <- 0 until nWays) {
    freeStall(i) := io.decode(i).valid && uops(i).regWriteEn && !freelist.io.allocPregs(i).valid
    robStall(i)  := io.decode(i).valid && !io.rob(i).ready // can't get the free rob space
  }
  val stallReq = freeStall.reduce(_ | _) || robStall.reduce(_ | _)

  for (i <- 0 until nWays) {
    // 当且仅当 来源数据有效 且 能够分配一组资源 且 外部无暂停信号，正式进行请求
    val valid = io.decode(i).valid && !stallReq && !io.stall

    io.dispatch(i).valid := valid

    freelist.io.requests(i) := valid && uops(i).regWriteEn
    rat.io.write(i).en      := valid && wRATen(i)

    rat.io.write(i).addr := uops(i).writeRegAddr
    rat.io.write(i).data := real(i).writeReg

    io.rob(i).valid := valid

    // To BusyTable
    io.checkIn(i).valid := valid && uops(i).regWriteEn
    io.checkIn(i).bits  := real(i).writeReg
  }

  io.stallReq := stallReq

  // To Dispatch Stage & ROB
  val dispatchData = Wire(Vec(nWays, new MicroOp(rename = true)))
  for (i <- 0 until nWays) {
    dispatchData(i)         := RemameUtil.uOpRename(uops(i), real(i))
    dispatchData(i).robAddr := io.rob(i).robAddr

    io.dispatch(i).bits := dispatchData(i)
    io.rob(i).bits      := RemameUtil.uOp2ROBEntry(uops(i), real(i))
  }

  // Commit signals
  for (i <- 0 until nWays) {
    freelist.io.commit   := io.commit.freelist
    rat.io.commit        := io.commit.rmt
    freelist.io.chToArch := io.commit.chToArch
    rat.io.chToArch      := io.commit.chToArch
  }

  // debug only not realse TODO
  io.rmtDebug := rat.io.debug.get

}

object RemameUtil {
  def uOp2ROBEntry(uop: MicroOp, re: RenameEntry): ROBEntry = {
    val robEntry = Wire(new ROBEntry)
    robEntry.pc        := uop.pc
    robEntry.complete  := 0.B
    robEntry.logicReg  := uop.writeRegAddr
    robEntry.physicReg := re.writeReg
    robEntry.originReg := re.originReg
    robEntry.exception := DontCare
    robEntry.instrType := uop.instrType
    robEntry.regWEn    := uop.regWriteEn
    robEntry.regWData  := DontCare
    robEntry.memWMode  := uop.storeMode
    robEntry.memWAddr  := DontCare
    robEntry.memWData  := DontCare
    robEntry.branch    := uop.bjCond =/= BJCond.none
    // robEntry.branch    := uop.bjCond =/= BJCond.none && uop.bjCond =/= BJCond.j && uop.bjCond =/= BJCond.jal
    robEntry.predictBT   := uop.predictBT
    robEntry.computeBT   := DontCare
    robEntry.inSlot      := uop.inSlot
    robEntry.branchTaken := DontCare

    robEntry.hiRegWrite  := DontCare
    robEntry.loRegWrite  := DontCare
    robEntry.cp0RegWrite := DontCare
    robEntry.cp0Addr     := uop.cp0RegAddr
    robEntry.cp0Sel      := uop.cp0RegSel
    robEntry.badvaddr    := DontCare
    robEntry.eret        := uop.eret

    robEntry
  }

  def uOpRename(uop: MicroOp, re: RenameEntry): MicroOp = {
    val renameUop = Wire(new MicroOp(rename = true))
    renameUop              := uop
    renameUop.rsAddr       := re.srcL
    renameUop.rtAddr       := re.srcR
    renameUop.writeRegAddr := re.writeReg

    renameUop
  }

}

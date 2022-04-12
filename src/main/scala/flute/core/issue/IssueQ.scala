package flute.core.issue

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.core.decode.{InstrType, MicroOp}
import flute.util.ValidBundle


class IQEntry extends Bundle {
  val microOp = new MicroOp
  val awake      = Bool()  // 书里的issued
  val selected   = Bool()
  val op1ShiftEn = Bool()
  val op1Shift   = SInt((delayMaxAmount+1).W)  // op1Shift(0)即op1Ready
  val op2ShiftEn = Bool()
  val op2Shift   = SInt((delayMaxAmount+1).W)  // op2Shift(0)即op2Ready
}

class SelectUnit extends Module {
  val io = IO(new Bundle{
    val request      = Input(UInt(log2Up(issueQEntryMaxAmount).W))
    val tobeSelected = Input(Vec(issueQEntryMaxAmount, new MicroOp))
    val selectedOp   = DecoupledIO(new MicroOp)
    val selectedIdx  = Output(UInt(log2Up(issueQEntryMaxAmount).W))
  })
  val selectedIdx = Wire(UInt(log2Up(issueQEntryMaxAmount).W))
  selectedIdx := PriorityEncoder(io.request)

  io.selectedOp.valid := io.request =/= 0.U
  io.selectedOp.bits  := io.tobeSelected(selectedIdx)
}

// A compress Queue with 2 enq port and 2 deq port
//
class IssueQ extends Module {
  val io = IO(new Bundle {
    val dataOut  = Output(Vec(issueQEntryMaxAmount, new IQEntry))
    val entryNum = Output(UInt(log2Up(issueQEntryMaxAmount).W))
    /// enqueue
    val enqData = Flipped(Vec(2, DecoupledIO(new IQEntry)))

    // provide that issueAddr(0) < issueAddr(1)
    val issueData = Output(Vec(2, new IQEntry))

    //debug
    val ctrl = Output(Vec(issueQEntryMaxAmount, UInt(3.W)))
  })
  val issueAddrBundle = Wire(Vec(2, ValidBundle(UInt(log2Up(issueQEntryMaxAmount).W))))

  val entries  = Mem(issueQEntryMaxAmount, new IQEntry)
  val entryNum = RegInit(0.U(log2Up(issueQEntryMaxAmount).W))

  object MoveState {
    val width = 3.W
    val stay       = 0.U(width)
    val preFirst   = 1.U(width)
    val preSecond  = 2.U(width)
    val readFirst  = 3.U(width)
    val readSecond = 4.U(width)
  }

  val ctrl = Wire(Vec(issueQEntryMaxAmount, UInt(3.W)))
  // debug
  io.ctrl := ctrl

  for (i <- 0 until issueQEntryMaxAmount) {
    val data = entries.read(i.U)
    val pre1 = if (i > issueQEntryMaxAmount - 1) data else entries.read((i + 1).U)
    val pre2 = if (i > issueQEntryMaxAmount - 2) data else entries.read((i + 2).U)

    entries.write(
      i.U,
      MuxLookup(
        key = ctrl(i),
        default = data,
        mapping = Seq(
          MoveState.stay       -> data,
          MoveState.preFirst   -> pre1,
          MoveState.preSecond  -> pre2,
          MoveState.readFirst  -> io.enqData(0).bits,
          MoveState.readSecond -> io.enqData(1).bits
        )
      )
    )
  }

  for (i <- 0 until 2) {
    io.issueData(i) := entries.read(issueAddrBundle(i).bits)
  }

  val numDeq    = PopCount(issueAddrBundle.map(_.valid)) // 下一拍出队数量，由仲裁单元保证合法
  val numAfterDeq = entryNum - numDeq

  // 内部ValidIO
  val enqAddr = Wire(Vec(2, ValidIO(UInt(log2Up(issueQEntryMaxAmount).W))))
  for (i <- 0 to 1) {
    val index = numAfterDeq + i.U
    enqAddr(i).bits  := index

    val inBound = Mux(index < issueQEntryMaxAmount.U, 1.B, 0.B)
    enqAddr(i).valid := inBound && io.enqData(i).valid

    // 同时给出enqData的ready信号
    // 该ready信号与enqData.valid信号独立,不会产生组合环
    io.enqData(i).ready := inBound
  }

  val numEnq = PopCount(io.enqData.map(_.valid))

  // 更新状态
  entryNum := entryNum + numEnq - numDeq
  io.entryNum := entryNum

  val issueAddr = Wire(Vec(2, UInt(5.W)))
  for (i <- 0 until 2) issueAddr(i) := Mux(issueAddrBundle(i).valid, issueAddrBundle(i).bits, issueQEntryMaxAmount.U)

  for (i <- 0 until issueQEntryMaxAmount) {
    ctrl(i) := MuxCase(
      default = MoveState.stay,
      mapping = Seq(
        (enqAddr(0).valid && i.U === enqAddr(0).bits)     -> MoveState.readFirst,
        (enqAddr(1).valid && i.U === enqAddr(1).bits)     -> MoveState.readSecond,
        (i.U < issueAddr(0))                              -> MoveState.stay,
        (i.U >= issueAddr(0) && i.U < issueAddr(1) - 1.U) -> MoveState.preFirst,
        (i.U >= issueAddr(1) - 1.U)                       -> MoveState.preSecond
      )
    )
  }

  for (i <- 0 until issueQEntryMaxAmount) {
    io.dataOut(i) := entries.read(i.U)
  }
  
  // Select //////////////////////////////////////////////////////
  val ALU0SelectUnit = new SelectUnit
  val ALU1SelectUnit = new SelectUnit
  val MDUSelectUnit  = new SelectUnit
  val LSUSelectUnit  = new SelectUnit

  val aluFlag = RegInit(0.B)
  aluFlag := ~aluFlag
  val request = Wire(Vec(issueQEntryMaxAmount, Bool()))

  for(i <- 0 to issueQEntryMaxAmount) {

    request(i) := (entries(i).microOp.op1.valid | entries(i).op1Shift(0)) &
      (entries(i).microOp.op2.valid | entries(i).op2Shift(0)) &
      entries(i).awake

    when(entries(i).microOp.instrType === InstrType.alu) {
      when(aluFlag === 0.B) {
        ALU0SelectUnit.io.request(i)      := request(i)
        ALU0SelectUnit.io.tobeSelected(i) := entries(i).microOp
        ALU1SelectUnit.io.request(i)      := 0.B
        MDUSelectUnit.io.request(i)       := 0.B
        LSUSelectUnit.io.request(i)       := 0.B
      }.otherwise {
        ALU0SelectUnit.io.request(i)      := 0.B
        ALU1SelectUnit.io.request(i)      := request(i)
        ALU1SelectUnit.io.tobeSelected(i) := entries(i).microOp
        MDUSelectUnit.io.request(i)       := 0.B
        LSUSelectUnit.io.request(i)       := 0.B
      }
    }.elsewhen(entries(i).microOp.instrType === InstrType.mulDiv) {
      ALU0SelectUnit.io.request(i)     := 0.B
      ALU1SelectUnit.io.request(i)     := 0.B
      MDUSelectUnit.io.request(i)      := request(i)
      MDUSelectUnit.io.tobeSelected(i) := entries(i).microOp
      LSUSelectUnit.io.request(i)      := 0.B
    }.elsewhen(entries(i).microOp.instrType === InstrType.loadStore) {
      ALU0SelectUnit.io.request(i)     := 0.B
      ALU1SelectUnit.io.request(i)     := 0.B
      MDUSelectUnit.io.request(i)      := 0.B
      LSUSelectUnit.io.request(i)      := request(i)
      LSUSelectUnit.io.tobeSelected(i) := entries(i).microOp
    }

  }

  io.issueData(0)          := ALU0SelectUnit.io.selectedOp.bits
  issueAddrBundle(0).bits  := ALU0SelectUnit.io.selectedIdx
  issueAddrBundle(0).valid := ALU0SelectUnit.io.selectedOp.valid

  io.issueData(1)          := ALU1SelectUnit.io.selectedOp.bits
  issueAddrBundle(1).bits  := ALU1SelectUnit.io.selectedIdx
  issueAddrBundle(1).valid := ALU1SelectUnit.io.selectedOp.valid

  // TODO: Add MDU/LSU Support Here
  //io.toMDU  <> MDUSelectUnit.io.selectedOp  // 乘除法单元
  //io.toLSU  <> LSUSelectUnit.io.selectedOp  // 读写单元

  ////////////////////////////////////////////////////////////////

  // Wakeup //////////////////////////////////////////////////////

  // TODO: Add MDU/LSU Support Here
  val alu0Grant = Wire(Vec(issueQEntryMaxAmount, Bool()))
  val alu1Grant = Wire(Vec(issueQEntryMaxAmount, Bool()))
  val dstBus    = Wire(Vec(2, ValidBundle(UInt(regAddrWidth.W))))
  val delayBus  = Wire(Vec(2, UInt((delayMaxAmount+1).W)))  // delayBus也用dstBus的valid

  // TODO: Add MDU/LSU Support Here
  for(i <- 0 to issueQEntryMaxAmount) {
    alu0Grant(i) := (ALU0SelectUnit.io.selectedIdx === i.U) & ALU0SelectUnit.io.selectedOp.valid
    alu1Grant(i) := (ALU1SelectUnit.io.selectedIdx === i.U) & ALU1SelectUnit.io.selectedOp.valid
    entries(i).awake := alu0Grant(i) | alu1Grant(i)
  }

  // TODO: Add MDU/LSU Support Here
  dstBus(0).valid := ALU0SelectUnit.io.selectedOp.valid
  dstBus(0).bits  := entries(ALU0SelectUnit.io.selectedIdx).microOp.writeRegAddr
  delayBus(0)     := 1.U << entries(ALU0SelectUnit.io.selectedIdx).microOp.delay
  dstBus(1).valid := ALU1SelectUnit.io.selectedOp.valid
  dstBus(1).bits  := entries(ALU1SelectUnit.io.selectedIdx).microOp.writeRegAddr
  delayBus(1)     := 1.U << entries(ALU1SelectUnit.io.selectedIdx).microOp.delay

  // TODO: Add MDU/LSU Support Here
  val busMatchOp1 = Wire(Vec(issueQEntryMaxAmount, UInt(2.W)))
  val busMatchOp2 = Wire(Vec(issueQEntryMaxAmount, UInt(2.W)))
  for(i <- 0 to issueQEntryMaxAmount) {
    for(j <- 0 to 2) {
      busMatchOp1(i)(j) := dstBus(j).valid & (dstBus(j).bits === entries(i).microOp.op1.op)
      busMatchOp2(i)(j) := dstBus(j).valid & (dstBus(j).bits === entries(i).microOp.op2.op)
    }

    entries(i).op1ShiftEn := busMatchOp1(i) =/= 0.U
    entries(i).op2ShiftEn := busMatchOp2(i) =/= 0.U

    entries(i).op1Shift := MuxLookup(
      key = busMatchOp1(i),
      default = 0.U,
      mapping = Seq(
        "b00".U -> 0.U,
        "b01".U -> delayBus(0),
        "b10".U -> delayBus(1)
      )
    )
    entries(i).op2Shift := MuxLookup(
      key = busMatchOp2(i),
      default = 0.U,
      mapping = Seq(
        "b00".U -> 0.U,
        "b01".U -> delayBus(0),
        "b10".U -> delayBus(1)
      )
    )

    when(entries(i).op1ShiftEn === true.B) {entries(i).op1Shift := entries(i).op1Shift >> 1}
  }


  ////////////////////////////////////////////////////////////////
}

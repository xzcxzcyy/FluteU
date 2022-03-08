package flute.core.decode.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp
import flute.core.decode.LoadMode
import flute.core.decode.StoreMode

class DecoupledSig extends Bundle {
  val valid = Output(Bool())
  val ready = Input(Bool())
}

class HazardCheck extends Module {
  val io = IO(new Bundle {
    val in  = Vec(2, Flipped(DecoupledIO(new MicroOp)))
    val out = Vec(2, new DecoupledSig)

    val query = Flipped(new QueryWBIO)
  })

  val uOps = for (i <- 0 until 2) yield io.in(i).bits

  // query bind
  io.query.addrIn(0) := uOps(0).rsAddr
  io.query.addrIn(1) := uOps(0).rtAddr
  io.query.addrIn(2) := uOps(1).rsAddr
  io.query.addrIn(3) := uOps(1).rtAddr
  io.query.addrIn(4) := uOps(1).writeRegAddr // 5号端口用于检查第二条指令的WAW

  val issueRdy = Wire(Vec(2, Bool()))
  val op1Rdy   = Wire(Vec(2, Bool()))
  val op2Rdy   = Wire(Vec(2, Bool()))

  for (i <- 0 until 2) {
    // i == 0 时, 查询0,1端口结果
    // i == 1 时, 查询2,3端口结果
    op1Rdy(i) := Mux(uOps(i).op1.valid, 1.B, io.query.dataOut(0 + 2 * i) === 0.U)
    op2Rdy(i) := Mux(uOps(i).op2.valid, 1.B, io.query.dataOut(1 + 2 * i) === 0.U)
  }

  // 第一条指令发射条件:操作数准备完毕
  issueRdy(0) := io.in(0).valid && op1Rdy(0) && op2Rdy(0)

  // 第二条指令

  // WAW check
  val wawWithPipeline = uOps(1).regWriteEn && (io.query.dataOut(4) =/= 0.U)
  val wawWithPreInstr = uOps(1).regWriteEn && uOps(0).regWriteEn &&
    (uOps(1).writeRegAddr === uOps(0).writeRegAddr)

  val waw = wawWithPipeline || wawWithPreInstr

  // RAW check
  // RAW with pipeline 已经隐含在操作数检查中;只需检查与第一条指令的RAW
  val rawWithOp1 = !uOps(1).op1.valid && uOps(1).rsAddr === uOps(0).writeRegAddr
  val rawWithOp2 = !uOps(1).op2.valid && uOps(1).rtAddr === uOps(0).writeRegAddr

  val raw = uOps(0).regWriteEn && (rawWithOp1 || rawWithOp2)

  // 访存 check: 第二条指令不能为访存指令 (TODO 改进)
  val load  = uOps(1).loadMode =/= LoadMode.disable
  val store = uOps(1).storeMode =/= StoreMode.disable

  val memUsed = load || store

  // 第二条指令发射条件:
  issueRdy(1) := io.in(1).valid && issueRdy(0) && !waw && !raw && !memUsed && op1Rdy(1) && op2Rdy(1)

  // R/V signals
  for (i <- 0 until 2) {
    io.out(i).valid := issueRdy(i)

    io.in(i).ready := io.out(i).ready && issueRdy(i)
  }
}

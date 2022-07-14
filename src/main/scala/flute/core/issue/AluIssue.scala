package flute.core.issue

import chisel3._
import chisel3.util._
import flute.core.decode.MicroOp
import flute.core.rename.BusyTableReadPort
import firrtl.FirrtlProtos.Firrtl.Expression.PrimOp.Op

class OpAwaken extends Bundle {
  private val deqNum = 2 // ALU 流水线个数

  val awaken = Bool()
  val sel    = UInt(log2Ceil(deqNum).W)
}
class AluEntry extends Bundle {
  val uop       = new MicroOp(rename = true)
  val op1Awaken = new OpAwaken
  val op2Awaken = new OpAwaken
}
class AluIssue(detectWidth: Int) extends Module {
  private val numOfAluPipeline = 2

  val io = IO(new Bundle {
    // 接收窗口
    val detect = Input(Vec(detectWidth, Valid(new MicroOp(rename = true))))
    // data from Issue Stage Reg
    val wake = Input(Vec(numOfAluPipeline, Valid(new AluEntry)))

    val bt = Vec(2 * detectWidth, Flipped(new BusyTableReadPort))

    val issue = Output(Vec(numOfAluPipeline, Valid(UInt(log2Ceil(detectWidth).W))))
    val out   = Output(Vec(numOfAluPipeline, Valid(new AluEntry)))
  })

  val avalible = Wire(Vec(detectWidth, Bool()))
  val awaken   = Wire(Vec(detectWidth, Bool()))

  val op1Awaken = Wire(Vec(detectWidth, new OpAwaken))
  val op2Awaken = Wire(Vec(detectWidth, new OpAwaken))

  val uops = VecInit(io.detect.map(_.bits))

  for (i <- 0 until detectWidth) {
    // avalible 能够发射（包括唤醒的指令）
    val bt = Wire(Vec(2, Bool()))

    io.bt(i * 2).addr     := uops(i).rsAddr
    io.bt(i * 2 + 1).addr := uops(i).rtAddr

    bt(0) := io.bt(i * 2).busy
    bt(1) := io.bt(i * 2 + 1).busy

    // awaken
    val op1AwakenByWho = Wire(Vec(numOfAluPipeline, Bool()))
    val op2AwakenByWho = Wire(Vec(numOfAluPipeline, Bool()))
    for (j <- 0 until numOfAluPipeline) {
      val (op1, op2) = AluIssueUtil.awake(io.wake(j).bits.uop, uops(i))
      op1AwakenByWho(j) := io.wake(j).valid && io.detect(i).valid && op1
      op2AwakenByWho(j) := io.wake(j).valid && io.detect(i).valid && op2
    }

    op1Awaken(i).awaken := op1AwakenByWho.reduce(_ | _)
    op1Awaken(i).sel    := OHToUInt(op1AwakenByWho)

    op2Awaken(i).awaken := op2AwakenByWho.reduce(_ | _)
    op2Awaken(i).sel    := OHToUInt(op2AwakenByWho)

    // 计算 avalible 与 awaken
    val op1Avalible = AluIssueUtil.op1Ready(uops(i), bt) || op1Awaken(i).awaken
    val op2Avalible = AluIssueUtil.op2Ready(uops(i), bt) || op2Awaken(i).awaken

    avalible(i) := op1Avalible && op2Avalible

    awaken(i) := avalible(i) && (op1Awaken(i).awaken || op2Awaken(i).awaken) // awaken 是 avalible的子集
  }

  // select 可优化为优先发送唤醒的指令，这里简单处理
  val canIssue = avalible

  val issue  = AluIssueUtil.selectFirstN(canIssue.asUInt, numOfAluPipeline)
  val issueV = issue.map({ case a => canIssue(a) })

  for (i <- 0 until numOfAluPipeline) {
    io.issue(i).bits  := issue(i)
    io.issue(i).valid := issueV(i) && io.detect(issue(i)).valid

    io.out(i).bits.uop       := uops(issue(i))
    io.out(i).bits.op1Awaken := op1Awaken(issue(i))
    io.out(i).bits.op2Awaken := op2Awaken(issue(i))
    io.out(i).valid          := issueV(i) && io.detect(issue(i)).valid
  }

}

object AluIssueUtil {
  def opAvalible(uop: MicroOp, bt: Seq[Bool]) = {
    assert(bt.length == 2)

    val r1PrfValid = uop.op1.valid || !bt(0)
    val r2PrfValid = uop.op2.valid || !bt(1)

    (r1PrfValid && r2PrfValid)
  }

  def op1Ready(uop: MicroOp, bt: Seq[Bool]) = {
    assert(bt.length == 2)

    uop.op1.valid || !bt(0)
  }

  def op2Ready(uop: MicroOp, bt: Seq[Bool]) = {
    assert(bt.length == 2)

    uop.op2.valid || !bt(1)
  }

  def awake(wake: MicroOp, uop: MicroOp) = {
    val isOp1Awaken = wake.regWriteEn &&
      wake.writeRegAddr === uop.rsAddr &&
      !uop.op1.valid
    val isOp2Awaken = wake.regWriteEn &&
      wake.writeRegAddr === uop.rtAddr &&
      !uop.op2.valid

    (isOp1Awaken, isOp2Awaken)
  }

  def selectFirstN(in: UInt, n: Int) = {
    val sels = Wire(Vec(n, UInt(log2Ceil(in.getWidth).W)))
    var mask = in

    for (i <- 0 until n) {
      sels(i) := PriorityEncoder(mask)
      val sel = UIntToOH(sels(i))
      mask = mask & ~sel
    }
    sels
  }
}

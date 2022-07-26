package flute.core.frontend

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.util._

import flute.config.CPUConfig._
import flute.cache.ICacheIO
import flute.util.BitMode.fromIntToBitModeLong
import flute.util.ValidBundle
import flute.cache.top.ICacheWithCore
import flute.core.backend.commit.BranchCommit
import flute.config.CPUConfig
import flute.cache.top.ICacheResp

class FetchIO extends Bundle {
  val ibufferEntries = Vec(decodeWay, Decoupled(new IBEntry))
}

class IBEntry extends Bundle {
  val inst      = UInt(instrWidth.W)
  val addr      = UInt(addrWidth.W)
  val inSlot    = Bool()
  val predictBT = UInt(addrWidth.W)
}

class FetchWithCP0 extends Bundle {
  val intrReq = Input(Bool())
  val eretReq = Input(Bool())
  val epc     = Input(UInt(dataWidth.W))
}

class Fetch extends Module {
  assert(fetchGroupSize == 2)

  private val pcQueueVolume: Int = 4

  val io = IO(new Bundle {
    val withDecode   = new FetchIO()
    val branchCommit = Input(new BranchCommit)
    val iCache       = Flipped(new ICacheWithCore)
    val cp0          = new FetchWithCP0
    val pc           = Output(UInt(addrWidth.W))
  })

  val pc        = RegInit(0.U(addrWidth.W))
  val bpc       = RegInit(0.U.asTypeOf(Valid(UInt(addrWidth.W))))
  val ib        = Module(new Ibuffer(new IBEntry, ibufferAmount, decodeWay, fetchGroupSize))
  val pcQ       = Module(new Queue(UInt(addrWidth.W), pcQueueVolume, hasFlush = true))
  val respStage = RegInit(0.U.asTypeOf(Valid(new ICacheResp)))
  val preDecs   = for (i <- 0 until fetchGroupSize) yield { Module(new PreDecode) }

  val extFlush      = io.branchCommit.pcRestore.valid || io.cp0.intrReq || io.cp0.eretReq
  val ibRoom        = PopCount(ib.io.write.map(_.ready))
  val ibPermitCache = ibRoom > 6.U
  val cacheReqReady = io.iCache.req.ready
  val pcQReady      = pcQ.io.enq.ready

  val pcRenewal     = ibPermitCache && cacheReqReady && pcQReady
  val cacheReqValid = ibPermitCache && pcQReady
  val pcQEnqValid   = ibPermitCache && cacheReqReady

  io.iCache.req.valid     := cacheReqValid
  io.iCache.req.bits.addr := pc

  pcQ.io.enq.valid := pcQEnqValid
  pcQ.io.enq.bits  := pc

}

object FetchUtil {
  def isLastInst(instAddr: UInt) = {
    assert(instAddr.getWidth == addrWidth)
    instAddr(4, 2) === "b111".U
  }
}

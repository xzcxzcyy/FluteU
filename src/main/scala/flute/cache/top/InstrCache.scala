package flute.cache.top

import chisel3._
import chisel3.util._
import flute.config.CPUConfig._
import flute.config.CacheConfig
import flute.cache.ram._
import flute.cache.components.TagValidBundle
import flute.cache.lru.LRU
import flute.cache.components.RefillUnit
import flute.axi.AXIIO
import flute.cache.axi.AXIBankRead

class ICacheReq extends Bundle {
  val addr = UInt(addrWidth.W)
}

class ICacheResp extends Bundle {
  val data = Vec(fetchGroupSize, UInt(dataWidth.W))
}

class ICacheWithCore extends Bundle {
  val req  = Flipped(DecoupledIO(new ICacheReq))
  val resp = ValidIO(new ICacheResp)
  val flush = Input(Bool())
}

class ThroughICache extends Module {
  implicit val config = new CacheConfig(numOfBanks = fetchGroupSize)

  val io = IO(new Bundle {
    val core = new ICacheWithCore
    val axi  = AXIIO.master()
  })

  val axiRead = Module(new AXIBankRead(axiId = 0.U))
  io.axi <> axiRead.io.axi

  axiRead.io.req.bits := io.core.req.bits.addr
  axiRead.io.req.ready <> io.core.req.ready
  axiRead.io.req.valid := io.core.req.valid

  val index = RegInit(0.U(config.bankIndexLen.W))
  when(io.core.req.fire) {
    index := config.getBankIndex(io.core.req.bits.addr)
  }

  for (i <- 0 until fetchGroupSize) {
    io.core.resp.bits.data(i) := axiRead.io.resp.bits(index + i.U)
  }
  io.core.resp.valid := axiRead.io.resp.valid

}

class InstrCache(cacheConfig: CacheConfig) extends Module {
  implicit val config = cacheConfig
  val nWays           = config.numOfWays
  val nSets           = config.numOfSets
  val nBanks          = config.numOfBanks

  val io = IO(new Bundle {
    val core = new ICacheWithCore
    val axi  = AXIIO.master()
  })

  val tagValid =
    for (i <- 0 until nWays) yield Module(new TypedSinglePortRam(nSets, new TagValidBundle))

  val instrBanks =
    for (i <- 0 until nWays)
      yield for (j <- 0 until nBanks) yield Module(new TypedSinglePortRam(nSets, UInt(dataWidth.W)))

  val lruUnit = LRU(nSets, nWays)

  val refillUnit = Module(new RefillUnit(AXIID = 0.U))

  val s0Valid = RegNext(io.core.req.fire, 0.B)
  val s0Addr  = RegEnable(io.core.req.bits.addr, io.core.req.fire)

  val vTag   = Wire(Vec(nWays, new TagValidBundle))
  val iBanks = Wire(Vec(nWays, Vec(nBanks, UInt(dataWidth.W))))
  for (i <- 0 until nWays) vTag(i) := tagValid(i).io.dataOut
  for (i <- 0 until nWays)
    for (j <- 0 until nBanks)
      iBanks(i)(j) := instrBanks(i)(j).io.dataOut

  val s1Stall  = WireDefault(0.B)
  val s1Enable = !s1Stall && s0Valid

  val s1Valid  = RegEnable(s0Valid, 0.B, s1Enable)
  val s1Addr   = RegEnable(s0Addr, s1Enable)
  val s1VTag   = RegEnable(vTag, s1Enable)
  val s1IBanks = RegEnable(iBanks, s1Enable)

  val reqTag       = config.getTag(s1Addr)
  val reqIndex     = config.getIndex(s1Addr)
  val reqBankIndex = config.getBankIndex(s1Addr)

  val tagHit = Wire(Vec(nWays, Bool()))
  for (i <- 0 until nWays) {
    tagHit(i) := s1VTag(i).valid && s1VTag(i).tag === reqTag
  }
  val hitInBanks = tagHit.reduce(_ || _)
  val hitWay     = OHToUInt(tagHit)

  val lastRefillAddrBuffer = Reg(UInt(32.W))
  val lastRefillAddrValid  = RegInit(0.B)

  val refillAddr = Cat(reqTag, reqIndex, reqBankIndex, 0.U(config.bankOffsetLen.W))

  val hitInRefillBuffer = lastRefillAddrValid && lastRefillAddrBuffer === refillAddr

  val hitData = Wire(Vec(fetchGroupSize, UInt(dataWidth.W)))
  for (i <- 0 until fetchGroupSize) {
    hitData(i) := MuxCase(
      0.U,
      Seq(
        hitInBanks        -> s1IBanks(hitWay)(reqBankIndex + i.U),
        hitInRefillBuffer -> refillUnit.io.data.bits(reqBankIndex + i.U)
      )
    )
  }

  val hit = s1Valid && (hitInBanks || hitInRefillBuffer)

  val miss = s1Valid && !(hitInBanks || hitInRefillBuffer)

  val missAddrBuffer = Reg(UInt(32.W))

  refillUnit.io.addr.valid := miss
  refillUnit.io.addr.bits  := refillAddr

  val piplining :: refilling :: writing :: Nil = Enum(3)
  val state                                    = RegInit(piplining)

  switch(state) {
    is(piplining) {
      when(miss) {
        state          := refilling
        missAddrBuffer := refillAddr
      }
    }
    is(refilling) {
      when(refillUnit.io.data.valid) {
        state                := writing
        lastRefillAddrBuffer := missAddrBuffer
        lastRefillAddrValid  := 1.B
      }
    }
    is(writing) {
      state := piplining
    }
  }

  io.core.req.ready := (state === piplining) && !miss

  s1Stall := !(state === piplining)

  // refillUnit.io.data.ready := (state === writing)

  val refillWay = lruUnit.getLRU(config.getIndex(missAddrBuffer))

  for (i <- 0 until nWays) {
    tagValid(i).io.addr := Mux(
      refillUnit.io.data.valid,
      config.getIndex(missAddrBuffer),
      config.getIndex(io.core.req.bits.addr)
    )
    tagValid(i).io.dataIn := Cat(config.getTag(missAddrBuffer), 1.B).asTypeOf(new TagValidBundle)
    tagValid(i).io.write  := refillUnit.io.data.valid && (i.U === refillWay)

    for (j <- 0 until nBanks) {
      instrBanks(i)(j).io.addr := Mux(
        refillUnit.io.data.valid,
        config.getIndex(missAddrBuffer),
        config.getIndex(io.core.req.bits.addr)
      )
      instrBanks(i)(j).io.dataIn := refillUnit.io.data.bits(j)
      instrBanks(i)(j).io.write  := refillUnit.io.data.valid && (i.U === refillWay)
    }
  }

  val missData = Wire(Vec(fetchGroupSize, UInt(dataWidth.W)))
  for (i <- 0 until fetchGroupSize) {
    missData(i) := refillUnit.io.data.bits(config.getBankIndex(missAddrBuffer) + i.U)
  }

  io.core.resp.valid := (state === piplining && hit) || (state === writing)
  io.core.resp.bits.data := MuxCase(
    VecInit(Seq.fill(fetchGroupSize)(0.U(32.W))),
    Seq(
      (state === writing)          -> missData,
      (state === piplining && hit) -> hitData
    )
  )

  io.axi <> refillUnit.io.axi
}

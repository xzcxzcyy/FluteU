package flute.axi

import chisel3._
import chisel3.util._

class AXIRamBlackBox(
    DATA_WIDTH: Int = 32,
    ADDR_WIDTH: Int = 32,
    ID_WIDTH: Int = 4,
    PIPELINE_OUTPUT: Int = 0
) extends BlackBox(
      Map(
        "DATA_WIDTH"      -> DATA_WIDTH,
        "ADDR_WIDTH"      -> ADDR_WIDTH,
        "STRB_WIDTH"      -> (DATA_WIDTH / 8),
        "ID_WIDTH"        -> ID_WIDTH,
        "PIPELINE_OUTPUT" -> PIPELINE_OUTPUT
      )
    )
    with HasBlackBoxResource {

  private val STRB_WIDTH = (DATA_WIDTH / 8)
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Reset())

    // aw
    val s_axi_awid    = Input(UInt(ID_WIDTH.W))
    val s_axi_awaddr  = Input(UInt(ADDR_WIDTH.W))
    val s_axi_awlen   = Input(UInt(8.W))
    val s_axi_awsize  = Input(UInt(3.W))
    val s_axi_awburst = Input(UInt(2.W))
    val s_axi_awlock  = Input(Bool())
    val s_axi_awcache = Input(UInt(4.W))
    val s_axi_awprot  = Input(UInt(3.W))
    val s_axi_awvalid = Input(Bool())
    val s_axi_awready = Output(Bool())

    // w
    val s_axi_wdata  = Input(UInt(DATA_WIDTH.W))
    val s_axi_wstrb  = Input(UInt(STRB_WIDTH.W))
    val s_axi_wlast  = Input(Bool())
    val s_axi_wvalid = Input(Bool())
    val s_axi_wready = Output(Bool())

    // b
    val s_axi_bid    = Output(UInt(ID_WIDTH.W))
    val s_axi_bresp  = Output(UInt(2.W))
    val s_axi_bvalid = Output(Bool())
    val s_axi_bready = Input(Bool())

    // ar
    val s_axi_arid    = Input(UInt(ID_WIDTH.W))
    val s_axi_araddr  = Input(UInt(ADDR_WIDTH.W))
    val s_axi_arlen   = Input(UInt(8.W))
    val s_axi_arsize  = Input(UInt(3.W))
    val s_axi_arburst = Input(UInt(2.W))
    val s_axi_arlock  = Input(Bool())
    val s_axi_arcache = Input(UInt(4.W))
    val s_axi_arprot  = Input(UInt(3.W))
    val s_axi_arvalid = Input(Bool())
    val s_axi_arready = Output(Bool())

    // r
    val s_axi_rid    = Output(UInt(ID_WIDTH.W))
    val s_axi_rdata  = Output(UInt(DATA_WIDTH.W))
    val s_axi_rresp  = Output(UInt(2.W))
    val s_axi_rlast  = Output(Bool())
    val s_axi_rvalid = Output(Bool())
    val s_axi_rready = Input(Bool())

  })
  addResource("/AXIRam.v")
}

class AXIRam extends Module {
  val io = IO(new Bundle {
    val axi = AXIIO.slave()
  })

  val axi_ram = Module(new AXIRamBlackBox(32, 32, 4, 0))
  axi_ram.io.clk := clock
  axi_ram.io.rst := reset

  // aw
  axi_ram.io.s_axi_awid    := io.axi.aw.bits.id
  axi_ram.io.s_axi_awaddr  := io.axi.aw.bits.addr
  axi_ram.io.s_axi_awlen   := io.axi.aw.bits.len
  axi_ram.io.s_axi_awsize  := io.axi.aw.bits.size
  axi_ram.io.s_axi_awburst := io.axi.aw.bits.burst
  axi_ram.io.s_axi_awlock  := io.axi.aw.bits.lock
  axi_ram.io.s_axi_awcache := io.axi.aw.bits.cache
  axi_ram.io.s_axi_awprot  := io.axi.aw.bits.prot
  axi_ram.io.s_axi_awvalid := io.axi.aw.valid
  io.axi.aw.ready          := axi_ram.io.s_axi_awready

  // w
  axi_ram.io.s_axi_wdata  := io.axi.w.bits.data
  axi_ram.io.s_axi_wstrb  := io.axi.w.bits.strb
  axi_ram.io.s_axi_wlast  := io.axi.w.bits.last
  axi_ram.io.s_axi_wvalid := io.axi.w.valid
  io.axi.w.ready          := axi_ram.io.s_axi_wready

  // b
  io.axi.b.bits.id        := axi_ram.io.s_axi_bid
  io.axi.b.bits.resp      := axi_ram.io.s_axi_bresp
  io.axi.b.valid          := axi_ram.io.s_axi_bvalid
  axi_ram.io.s_axi_bready := io.axi.b.ready

  // ar
  axi_ram.io.s_axi_arid    := io.axi.ar.bits.id
  axi_ram.io.s_axi_araddr  := io.axi.ar.bits.addr
  axi_ram.io.s_axi_arlen   := io.axi.ar.bits.len
  axi_ram.io.s_axi_arsize  := io.axi.ar.bits.size
  axi_ram.io.s_axi_arburst := io.axi.ar.bits.burst
  axi_ram.io.s_axi_arlock  := io.axi.ar.bits.lock
  axi_ram.io.s_axi_arcache := io.axi.ar.bits.cache
  axi_ram.io.s_axi_arprot  := io.axi.ar.bits.prot
  axi_ram.io.s_axi_arvalid := io.axi.ar.valid
  io.axi.ar.ready          := axi_ram.io.s_axi_arready

  // r
  io.axi.r.bits.id        := axi_ram.io.s_axi_rid
  io.axi.r.bits.data      := axi_ram.io.s_axi_rdata
  io.axi.r.bits.resp      := axi_ram.io.s_axi_rresp
  io.axi.r.bits.last      := axi_ram.io.s_axi_rlast
  io.axi.r.valid          := axi_ram.io.s_axi_rvalid
  axi_ram.io.s_axi_rready := io.axi.r.ready

}

object Gen extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new AXIRam, Array("--target-dir", "target/verilog/axiram", "--target:fpga"))
}

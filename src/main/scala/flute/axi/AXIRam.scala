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

}

object Gen extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new AXIRam, Array("--target-dir", "target/verilog", "--target:fpga"))
}

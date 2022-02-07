package pipeline


import chisel3._
import chisel3.util.MuxLookup
import components.ALU
import config.CpuConfig._
import mock.MockInstrMem


class Fetch extends Module{
    val io = IO(new Bundle {
        val branchTaken = Input(Bool())
        val branchAddr = Input(UInt(addrWidth.W))

        val pcEnable = Input(Bool())

        val toDecode = Output(new IfIdBundle)
    })

    val pc = RegInit(0.U(instrWidth.W))  // for tmp, pc should be finally init to a start-up

    val instrMem = Module(new MockInstrMem("./test_data/mem.in"))

    instrMem.io.addr := pc
    io.toDecode.instruction := instrMem.io.dataOut

    val pcplusfour = pc + 4.U

    io.toDecode.pcplusfour := pcplusfour

    when(io.pcEnable) {
        pc := Mux(io.branchTaken, io.branchAddr, pcplusfour)
    }.otherwise {
        pc := pc
    }


}
package core.pipeline

import chisel3._
import chisel3.util.MuxLookup
import config.CpuConfig._
import core.components.ALU
import core.pipeline.stagereg.IfIdBundle
import mock.MockInstrMem


class Fetch extends Module{
    val io = IO(new Bundle {
        val branchTaken = Input(Bool())
        val branchAddr = Input(UInt(addrWidth.W))
        val pcStall = Input(Bool())

        val toDecode = Output(new IfIdBundle)
        val iMemStallReq = Output(Bool())
    })

    val pc = RegInit(0.U(instrWidth.W))  // for tmp, pc should be finally init to a start-up

    val instrMem = Module(new MockInstrMem("./test_data/imem.in"))

    instrMem.io.addr := pc
    io.iMemStallReq := !instrMem.io.valid
    
    val pcplusfour = pc + 4.U
    io.toDecode.pcplusfour := pcplusfour
    io.toDecode.instruction := instrMem.io.dataOut

    when(!io.pcStall) {
        pc := Mux(io.branchTaken, io.branchAddr, pcplusfour)
    }.otherwise {
        pc := pc
    }

}
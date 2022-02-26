package flute.core

import chisel3._

import flute.config._
import flute.cache._
import flute.core.fetch._
import flute.core.decode._
import flute.core.execute._

class Core(implicit conf: CPUConfig) extends MultiIOModule {
    val io = IO(new Bundle {
        val iCache  = new ICacheIO()
        val dCache  = new DCacheIO()
    })

    val fetch   = Module(new Fetch())
    val decode  = Module(new Decode())
    val execute = Module(new Execute())

    fetch.io.next       := decode.io.fetch
    decode.io.next      := execute.io.decode
    fetch.io.feedback   := execute.io.fetch

    io.iCache := fetch.io.iCache
    io.dCache := execute.io.dCache
}
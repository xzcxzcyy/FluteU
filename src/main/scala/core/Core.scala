package core

import chisel3._

import config.CpuConfig._
import cache._
import core.fetch._
import core.decode._
import core.execute._

class Core extends Module {
  val io = IO(new Bundle {
    val iCache = new ICacheIO
    val dCache = new DCacheIO
  })

  val fetch     = Module(new Fetch())
  val decode    = Module(new Decode())
  val executors = for (i <- 1 to superscalar) yield Module(new Execute())

  fetch.io.next := decode.io.fetch

  for (i <- 0 to superscalar - 1) yield {
    decode.io.next.executors(i)    := executors(i).io.execute
    // fetch.io.feedback.executors(i) := executors(i).io.feedback
    io.dCache                      := executors(i).io.dCache
  }

  io.iCache := fetch.io.iCache
}

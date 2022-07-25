package flute.core.backend.rename

import flute.util.BaseTestHelper
import firrtl.AnnotationSeq
import treadle.TreadleTester

import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.stage._
import firrtl.options.TargetDirAnnotation
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import flute.config.CPUConfig._

private class TestHelper(fileName: String)
    extends BaseTestHelper(fileName, () => new Freelist(2, 2)) {
  def peekPort() = {
    val freelist    = t.peek(s"io_debug_freelist")
    val selMask     = t.peek(s"io_debug_selMask")
    val deallocMask = t.peek(s"io_debug_deallocMask")
    val next        = t.peek(s"io_debug_next")
    val empty       = t.peek(s"io_empty")
    writer.println(s"freelist=${"0x%08x".format(freelist)} selMask=${selMask} deallocMask=${"0x%08x"
      .format(deallocMask)} next=${"0x%08x".format(next)} empty=${empty}")

    for (i <- 0 until 2) {
      val sel = t.peek(s"io_debug_sels_${i}")
      writer.println(s"sels${i}=${sel}")
    }
  }

  def defaultPock() = {
    for (i <- 0 until 2) {
      t.poke(s"io_requests_${i}", bool2BigInt(true))
      t.poke(s"io_commit_alloc_${i}_valid", bool2BigInt(false))
      t.poke(s"io_commit_free_${i}_valid", bool2BigInt(false))
    }
  }

  def dellocPock() = {
    for (i <- 0 until 2) {
      t.poke(s"io_requests_${i}", bool2BigInt(false))
      t.poke(s"io_commit_free_${i}_valid", bool2BigInt(true))
      t.poke(s"io_commit_free_${i}_bits", i + 1)
    }
  }
}

class FreelistTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "test" in {
    val t = new TestHelper("freelist_gen")

    t.peekPort()

    t.step()

    t.peekPort()

    t.defaultPock()

    for (i <- 0 until 36) {
      t.step()
      t.peekPort()
    }

    t.t.poke(s"io_chToArch", t.bool2BigInt(true))
    t.step()
    t.peekPort()

    // t.dellocPock()
    // t.step()
    // t.peekPort()

    t.close()

  }

}

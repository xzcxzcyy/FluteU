package flute.core.fetch

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers
import flute.cache.ICache
import flute.config.CPUConfig._
import flute.util.BitMode.fromIntToBitModeLong
import flute.core.execute.ExecuteFeedbackIO
import firrtl.options.TargetDirAnnotation
import chisel3.stage.ChiselGeneratorAnnotation
import treadle.TreadleTester
import chisel3.stage.ChiselStage

class FetchTest extends AnyFreeSpec with Matchers {
  "FetchTestTop" in {
    val firrtlAnno = (new ChiselStage).execute(
      Array(),
      Seq(
        TargetDirAnnotation("target"),
        ChiselGeneratorAnnotation(() => new FetchTestTop("fetch1_base"))
      )
    )

    val t = TreadleTester(firrtlAnno)
    t.report()
  }
}

class FetchTestTop(file: String = "") extends Module {
  val io = IO(new Bundle {
    val withDecode       = new FetchIO
    val feedbackFromExec = Flipped(new ExecuteFeedbackIO)
  })
  val iCache = Module(new ICache(s"target/clang/${file}.hexS"))
  val fetch  = Module(new Fetch)
  fetch.io.iCache <> iCache.io
  io.withDecode <> fetch.io.withDecode
  fetch.io.feedbackFromExec <> io.feedbackFromExec
}

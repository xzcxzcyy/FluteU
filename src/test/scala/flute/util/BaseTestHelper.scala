package flute.util

import java.io.PrintWriter
import java.io.File
import firrtl.AnnotationSeq
import treadle.TreadleTester
import chisel3.RawModule
import chisel3.stage.ChiselStage
import firrtl.options.TargetDirAnnotation
import chisel3.stage.ChiselGeneratorAnnotation

abstract class BaseTestHelper(logName: String, gen: () => RawModule) {

  val log = new File(s"target/log/${logName}.log")
  log.getParentFile().mkdirs()
  val writer              = new PrintWriter(log)
  def fprintln(s: String) = writer.println(s)

  val firrtlAnno: AnnotationSeq = (new ChiselStage).execute(
    Array(),
    Seq(
      TargetDirAnnotation("target"),
      ChiselGeneratorAnnotation(gen)
    )
  )
  val t: TreadleTester = TreadleTester(firrtlAnno)

  val poke  = t.poke _
  val peek  = t.peek _
  var clock = 0
  def step(n: Int = 1) = {
    t.step(n)
    clock += n
    writer.println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
    println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
  }
  def close() = {
    writer.close()
  }

}

package flute.util

import java.io.PrintWriter
import java.io.File
import firrtl.AnnotationSeq
import treadle.TreadleTester

abstract class BaseTestHelper(logName: String) {

  val log = new File(s"target/log/${logName}.log")
  log.getParentFile().mkdirs()
  val writer = new PrintWriter(log)

  val firrtlAnno: AnnotationSeq

  val t     = TreadleTester(firrtlAnno)
  val poke  = t.poke _
  val peek  = t.peek _
  var clock = 0
  def step(n: Int = 1) = {
    t.step(n)
    clock += n
    writer.println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
    println(s">>>>>>>>>>>>>>>>>> Total clock steped: ${clock} ")
  }

}

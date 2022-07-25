package flute.core.backend

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class BackendTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  
  it should "compile" in {
    test(new Backend).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      
    }
  }

}
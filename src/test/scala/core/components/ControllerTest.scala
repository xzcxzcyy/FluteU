package core.components

import chisel3._
import chiseltest._
import core.decode.Controller
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ControllerTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "AND Test" in {
    test(new Controller()) { c =>

    }
  }
}

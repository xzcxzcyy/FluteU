package flute.core.backend.rename

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

private class RenameTestHelper(fileName: String)
    extends BaseTestHelper(fileName, () => new Rename(4, 4)) {

    
}

class RenameTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "compile" in {
    val test = new RenameTestHelper("RenameTset")
    test.writer.println("empty")
  }
}

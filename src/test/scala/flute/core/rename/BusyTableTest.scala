package flute.core.rename

import flute.util.BaseTestHelper
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.ChiselScalatestTester
import org.scalatest.matchers.should.Matchers

private class BusyTableTestHelper(fileName: String)
    extends BaseTestHelper(fileName, () => new BusyTable(12, 2, 2)) {

  def pokeCheckIn(addr1: Int, addr2: Int) = {
    for (i <- 0 until 2) {
      t.poke(s"io_checkIn_${i}_valid", bool2BigInt(true))
    }
    t.poke(s"io_checkIn_${0}_bits", addr1)
    t.poke(s"io_checkIn_${1}_bits", addr2)
  }

  def shutPokeCheckIn() = {
    for (i <- 0 until 2) {
      t.poke(s"io_checkIn_${i}_valid", bool2BigInt(false))
    }
  }

  def pokeCheckOut(addr1: Int, addr2: Int) = {
    for (i <- 0 until 2) {
      t.poke(s"io_checkOut_${i}_valid", bool2BigInt(true))
    }
    t.poke(s"io_checkOut_${0}_bits", addr1)
    t.poke(s"io_checkOut_${1}_bits", addr2)
  }

  def shutPokeCheckOut() = {
    for (i <- 0 until 2) {
      t.poke(s"io_checkOut_${i}_valid", bool2BigInt(false))
    }
  }

  def peekDebug() = {
    val table = t.peek(s"io_debug_table")
    writer.println(s"table = ${"0x%x".format(table)}")

    // peek read
    for(i <- 0 until 2) {
      t.poke(s"io_read_${i}_addr", i + 2)
      val busy = t.peek(s"io_read_${i}_busy")
      writer.println(s"busy_${i} = ${busy}")
    }
  }
}

class BusyTableTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  "compile" in {
    val t = new BusyTableTestHelper("BusyTableTset")

    t.peekDebug()

    t.shutPokeCheckIn()
    t.shutPokeCheckOut()

    t.pokeCheckIn(1, 2)
    t.step()
    t.peekDebug()

    t.pokeCheckIn(3, 4)
    t.step()
    t.peekDebug()

    t.pokeCheckIn(5, 6)
    t.pokeCheckOut(1, 2)
    t.step()
    t.peekDebug()

    t.writer.close()
  }
}

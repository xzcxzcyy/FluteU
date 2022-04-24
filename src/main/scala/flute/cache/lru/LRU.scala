package flute.cache.lru

import chisel3._
import chisel3.util._

abstract class BaseLRU {
  def update(index: UInt, way: UInt): Unit

  def getLRU(index: UInt): UInt

}

class LRU(numOfSets: Int, numOfWay: Int, searchOrder: Boolean = false) extends BaseLRU {
  // require(numOfWay >= 4, "number of way should not be equal to 2")
  require(isPow2(numOfWay), "number of way should be a power of 2")
  require(isPow2(numOfSets), "number of sets should be a power of 2")

  val lruReg = RegInit(VecInit(Seq.fill(numOfSets)(VecInit(Seq.fill(numOfWay)(false.B)))))

  override def update(index: UInt, way: UInt): Unit = {
    val setMRUWire = WireDefault(lruReg(index))
    setMRUWire(way) := true.B
    val invertedWire = WireDefault(0.U.asTypeOf(setMRUWire))
    invertedWire(way) := true.B
    lruReg(index)     := Mux(setMRUWire.asUInt.andR, invertedWire, setMRUWire)
  }

  override def getLRU(index: UInt): UInt = {
    val setMRUWire = WireDefault(lruReg(index))
    if (searchOrder) {
      setMRUWire.indexWhere((isMRU => !isMRU))
    } else {
      setMRUWire.lastIndexWhere((isMRU => !isMRU))
    }
  }
}

class LRU1Bit(numOfSets: Int, numOfWay: Int, searchOrder: Boolean = false) extends BaseLRU {
  require(numOfWay == 2, "number of way should not be 2. We have a true LRU for 2")

  val lruLine = RegInit(VecInit(Seq.fill(numOfSets)(0.U(1.W))))

  override def update(index: UInt, way: UInt): Unit = {
    assert(way.getWidth == 1, "true LRU should have a way width of 1")
    lruLine(index) := way
  }

  override def getLRU(index: UInt): UInt = {
    lruLine(index)
  }
}

object LRU {
  def apply(numOfSets: Int, numOfWay: Int, searchOrder: Boolean = false): BaseLRU = if (
    numOfWay == 2
  ) new LRU1Bit(numOfSets, numOfWay, searchOrder)
  else new LRU(numOfSets, numOfWay, searchOrder)
}

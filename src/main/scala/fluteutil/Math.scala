package fluteutil

object Math {
  def log2Up(x: Int): Int = {
    assert(x >= 0)
    var w  = 0
    var it = x - 1
    while (it > 0) {
      w += 1
      it /= 2
    }
    w
  }
}

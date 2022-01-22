package fluteutil

object BitMode {
  implicit class fromIntToBitModeLong(number: Int) {
    def BM: Long = {
      if (number >= 0) {
        number.toLong
      } else {
        (1L << 32) + number.toLong
      }
    }
  }
}

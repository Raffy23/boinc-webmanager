package at.happywetter.boinc.shared.util

/**
 * Created by: 
 *
 * @author Raphael
 * @version 01.02.2020
 */
object LexicographicStringOrdering extends Ordering[String] {

  override def compare(left: String, right: String): Int = {
    val leftLength  = left.length
    val rightLength = right.length

    var i=0
    while (i < leftLength && i < rightLength) {
      val diff = left(i) - right(i)

      if (diff != 0) {
        return diff
      }

      i += 1
    }

    if (leftLength < rightLength)
      leftLength - rightLength
    else if (leftLength > rightLength)
      rightLength - leftLength
    else
      0
  }

}

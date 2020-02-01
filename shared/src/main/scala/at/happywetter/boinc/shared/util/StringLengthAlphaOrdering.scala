package at.happywetter.boinc.shared.util

/**
 * Created by: 
 *
 * @author Raphael
 * @version 01.02.2020
 */
object StringLengthAlphaOrdering extends Ordering[String] {

  def compare(left: String, right: String): Int = {
    val lenDiff = left.length - right.length

    if (lenDiff == 0) left.compareTo(right)
    else lenDiff
  }

}

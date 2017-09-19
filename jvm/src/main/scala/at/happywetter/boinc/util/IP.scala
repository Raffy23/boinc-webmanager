package at.happywetter.boinc.util

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.09.2017
  */
case class IP(value : Int) {

  private def a: Int = (value >> 24) & 255
  private def b: Int = (value >> 16) & 255
  private def c: Int = (value >> 8) & 255
  private def d: Int = value & 255

  override def toString = s"$a.$b.$c.$d"

  def increment = IP(value + 1)

  def to(ip: IP): Iterable[IP] = (value to ip.value).map(itr => IP(itr))
  def until(ip: IP): Iterable[IP] = (value until ip.value).map(itr => IP(itr))
}

object IP {

  private val Pattern = """^(\d{1,3}).(\d{1,3}).(\d{1,3}).(\d{1,3})$""".r

  def apply(str: String): IP = str match {
    case Pattern(a, b, c, d) => IP((a.toInt << 24) + (b.toInt << 16) + (c.toInt << 8) + d.toInt)
  }

}
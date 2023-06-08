package at.happywetter.boinc.web.css

import scala.collection.mutable

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object GlobalRegistry:

  sealed trait Mode:
    protected[css] def encodeIdentifier(identifier: String): String

  object DevMode extends Mode:
    override protected[css] def encodeIdentifier(identifier: String): String =
      val result = identifier
      lookup += 1

      result

  object ProdMode extends Mode:
    override protected[css] def encodeIdentifier(identifier: String): String =
      val result = "_" + lookup.toHexString
      lookup += 1

      result

  private val registry = new mutable.HashMap[String, String]()
  private var lookup = 0L

  def registerDefinitions(is: StyleDefinitions*)(implicit mode: Mode): Int =
    is.flatMap(_.styles).map(register).count(_ => true)

  def register(identifier: CSSIdentifier*)(implicit mode: Mode): Unit =
    identifier.foreach(register)

  def register(identifier: CSSIdentifier)(implicit mode: Mode): Unit =
    registry.put(identifier.name, mode.encodeIdentifier(identifier.name))

  def getCSSName(identifier: CSSIdentifier): String = registry(identifier.name)

  protected[css] def _getCSSName(identifier: String): String = registry(identifier)

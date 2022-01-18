package at.happywetter.boinc.util.http4s

import cats.effect.IO
import org.http4s.{EntityBody, EntityEncoder}

import scala.language.higherKinds
import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 09.07.2019
  */
object Implicits {

  implicit def convertStringToEntityBody(str: String): EntityBody[IO] =
    EntityEncoder.stringEncoder.toEntity(str).body

  implicit def convertByteArrayToEntityBody(array: Array[Byte]): EntityBody[IO] =
    EntityEncoder.byteArrayEncoder.toEntity(array).body

}

package at.happywetter.boinc.util.http4s

import scala.language.higherKinds
import scala.language.implicitConversions

import cats.effect.IO
import org.http4s.{Entity, EntityBody, EntityEncoder}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 09.07.2019
  */
object Implicits:

  implicit def convertStringToEntityBody(str: String): Entity[IO] =
    EntityEncoder.stringEncoder.toEntity(str)

  implicit def convertByteArrayToEntityBody(array: Array[Byte]): Entity[IO] =
    EntityEncoder.byteArrayEncoder.toEntity(array)

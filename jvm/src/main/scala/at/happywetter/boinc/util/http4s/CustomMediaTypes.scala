package at.happywetter.boinc.util.http4s

import org.http4s.MediaType
import org.http4s.MediaType.Binary
import org.http4s.MediaType.Compressible

object CustomMediaTypes:

  val messagepack = new MediaType("application", "messagepack", Compressible, Binary)

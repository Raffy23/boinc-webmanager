package at.happywetter.boinc.web.util

import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.js.typedarray.{ArrayBuffer, DataView}
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import at.happywetter.boinc.web.facade.TextEncoder

object Crypto:

  def hashPassword(password: String, nonce: String): Future[String] =
    dom.crypto.subtle
      .digest(dom.HashAlgorithm.`SHA-256`, new TextEncoder("utf-8").encode(nonce + password).buffer)
      .toFuture
      .map(buffer => {
        val hex = new StringBuilder()
        val view = new DataView(buffer.asInstanceOf[ArrayBuffer])
        for (i <- 0 until view.byteLength by 2) {
          hex.append(view.getUint16(i).toHexString.reverse.padTo(4, '0').reverse)
        }

        hex.toString()
      })

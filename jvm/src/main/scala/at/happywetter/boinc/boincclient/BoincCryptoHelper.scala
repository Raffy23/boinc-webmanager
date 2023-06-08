package at.happywetter.boinc.boincclient

import java.math.BigInteger
import java.security.MessageDigest

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.08.2017
  */
object BoincCryptoHelper:

  def md5(str: String): String =
    // http://web.archive.org/web/20140209230440/http://www.sergiy.ca/how-to-make-java-md5-and-sha-1-hashes-compatible-with-php-or-mysql/
    val hash = new BigInteger(1, MessageDigest.getInstance("MD5").digest(str.getBytes("UTF-8")))
    var result = hash.toString(16)

    while result.length() < 32 do // 40 for SHA-1
      result = "0" + result

    result

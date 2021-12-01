package at.happywetter.boinc.util.http4s

import java.io.FileInputStream
import java.security.KeyStore
import at.happywetter.boinc.AppConfig.Config
import org.http4s.blaze.server.BlazeServerBuilder

import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 27.02.2020
 */
object CustomBlazeServerBuilder {

  implicit class SSLContextBlazeServerBuilder[F[_]](val sb: BlazeServerBuilder[F]) extends AnyVal {

    def withOptionalSSL(config: Config): BlazeServerBuilder[F] = {
      if (config.server.ssl.enabled) {
        val trustStoreInputStream = new FileInputStream(config.server.ssl.keystore)
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType)
        trustStore.load(trustStoreInputStream, config.server.ssl.password.toCharArray)
        trustStoreInputStream.close()

        val keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
        keyManager.init(trustStore, config.server.ssl.password.toCharArray)

        val trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
        trustManager.init(trustStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManager.getKeyManagers, trustManager.getTrustManagers, null)

        sb.withSslContext(sslContext)
      } else {
        sb
      }
    }
  }

}

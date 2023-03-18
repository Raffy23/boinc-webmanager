package at.happywetter.boinc.util.http4s

import at.happywetter.boinc.AppConfig.Config
import cats.effect.Async
import fs2.io.net.tls.TLSContext
import org.http4s.ember.server.EmberServerBuilder

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 27.02.2020
 */
object CustomEmberServerBuilder {

  implicit class SSLContextBlazeServerBuilder[F[_]: Async](private val sb: EmberServerBuilder[F]) {

    def withOptionalSSL(config: Config): EmberServerBuilder[F] = {
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

        sb.withTLS(TLSContext.Builder.forAsync[F].fromSSLContext(sslContext))
      } else {
        sb
      }
    }
  }

}

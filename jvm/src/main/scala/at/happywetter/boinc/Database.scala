package at.happywetter.boinc

import at.happywetter.boinc.repository.{CoreClientRepository, JobRepository, ProjectRepository}
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory
import io.getquill.{H2JdbcContext, SnakeCase}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
 * Created by: 
 *
 * @author Raphael
 * @version 02.07.2020
 */
class Database private (ctx: H2JdbcContext[SnakeCase], logger: SelfAwareStructuredLogger[IO]) extends AutoCloseable {

  val clients  = new CoreClientRepository(ctx)
  val projects = new ProjectRepository(ctx)
  val jobs = new JobRepository(ctx)

  override def close(): Unit = {
    logger.trace("Closing database connection ...").unsafeRunSync()
    ctx.close()
  }

}
object Database {

  def apply(): Resource[IO, Database] =
    Resource.fromAutoCloseable(for {
      logger <- Slf4jLogger.fromClass[IO](getClass)

      _ <- logger.trace("Connecting to database ...")

      database <- IO.blocking {
        new Database(
          new H2JdbcContext(
            SnakeCase,
            ConfigFactory
              .parseResources("database/database.conf")
              .resolveWith(AppConfig.typesafeConfig)
          ),
          logger
        )
      }

      _ <- logger.trace("Connection established")
    } yield database)

}

package at.happywetter.boinc

import at.happywetter.boinc.repository.{CoreClientRepository, JobRepository, ProjectRepository}
import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import doobie.h2.H2Transactor
import doobie.hikari.HikariTransactor
import doobie.{ExecutionContexts, Transactor}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.Properties

/**
 * Created by: 
 *
 * @author Raphael
 * @version 02.07.2020
 */
class Database private (xa: Transactor[IO], logger: SelfAwareStructuredLogger[IO]):

  val clients = new CoreClientRepository(xa)
  val projects = new ProjectRepository(xa)
  val jobs = new JobRepository(xa)

object Database:

  def apply(): Resource[IO, Database] =
    for {
      logger <- Resource.eval(Slf4jLogger.fromClass[IO](getClass))

      ce <- ExecutionContexts.fixedThreadPool[IO](4)
      xa <- HikariTransactor.fromHikariConfig[IO](
        {
          val properties = new Properties()

          ConfigFactory
            .parseResources("database/database.conf")
            .resolveWith(AppConfig.typesafeConfig)
            .resolve()
            .entrySet()
            .forEach(entry => {
              properties.put(entry.getKey, entry.getValue.unwrapped())
            })

          new HikariConfig(properties)
        },
        ce
      )

      database <- Resource.eval(
        IO.pure(new Database(xa, logger))
      )
    } yield database

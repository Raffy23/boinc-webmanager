package at.happywetter.boinc

import at.happywetter.boinc.repository.{CoreClientRepository, ProjectRepository}
import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory
import io.getquill.{H2JdbcContext, SnakeCase}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 02.07.2020
 */
class Database private (ctx: H2JdbcContext[SnakeCase]) extends AutoCloseable {

  val clients  = new CoreClientRepository(ctx)
  val projects = new ProjectRepository(ctx)
  //val jobs = ???

  override def close(): Unit = ctx.close()

}
object Database {

  def apply(): Resource[IO, Database] =
    Resource.fromAutoCloseable(IO.blocking {
      new Database(
        new H2JdbcContext(
          SnakeCase,
          ConfigFactory
            .parseResources("database/database.conf")
            .resolveWith(AppConfig.typesafeConfig)
        )
      )
    })

}

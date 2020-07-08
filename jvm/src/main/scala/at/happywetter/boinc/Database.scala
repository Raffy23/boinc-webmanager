package at.happywetter.boinc

import at.happywetter.boinc.repository.{CoreClientRepository, ProjectRepository}
import at.happywetter.boinc.util.IOAppTimer
import cats.effect.{ContextShift, IO, Resource}
import com.typesafe.config.ConfigFactory
import io.getquill.context.monix.Runner
import io.getquill.{H2MonixJdbcContext, SnakeCase}
import monix.execution.Scheduler

/**
 * Created by: 
 *
 * @author Raphael
 * @version 02.07.2020
 */
class Database private (ctx: H2MonixJdbcContext[SnakeCase]) extends AutoCloseable {

  val clients  = new CoreClientRepository(ctx)
  val projects = new ProjectRepository(ctx)

  override def close(): Unit = ctx.close()

}
object Database {

  def apply()(implicit contextShift: ContextShift[IO]): Resource[IO, Database] =
    Resource.fromAutoCloseableBlocking(IOAppTimer.blocker)(IO {
      new Database(
        new H2MonixJdbcContext(
          SnakeCase,
          ConfigFactory
            .parseResources("database/database.conf")
            .resolveWith(AppConfig.typesafeConfig),
          Runner.using(Scheduler.io())
        )
      )
    })

}

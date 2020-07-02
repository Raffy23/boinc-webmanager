package at.happywetter.boinc

import at.happywetter.boinc.util.{IOAppTimer, IP}
import cats.data.OptionT
import cats.effect.{ContextShift, IO, Resource}
import com.typesafe.config.ConfigFactory
import io.getquill.context.monix.Runner
import io.getquill.{H2MonixJdbcContext, SnakeCase}
import monix.eval.Task
import monix.execution.Scheduler

/**
 * Created by: 
 *
 * @author Raphael
 * @version 02.07.2020
 */
class Database private (ctx: H2MonixJdbcContext[SnakeCase]) extends AutoCloseable {
  import at.happywetter.boinc.dto.DatabaseDTO._
  import ctx._

  def insert(coreClient: CoreClient): Task[Long] = run {
    quote {
      query[CoreClient].insert(lift(coreClient))
    }
  }

  def searchCoreClientBy(ip: IP): OptionT[Task, CoreClient] = OptionT(
    run {
      quote {
        query[CoreClient].filter(_.ipAddress == lift(ip.toString))
      }
    }.map(_.headOption)
  )

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

package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.util.IP
import cats.data.OptionT
import io.getquill.{H2MonixJdbcContext, SnakeCase}
import monix.eval.Task

/**
 * Created by: 
 *
 * @author Raphael
 * @version 08.07.2020
 */
class CoreClientRepository(ctx: H2MonixJdbcContext[SnakeCase]) {
  import ctx._

  def insert(coreClient: CoreClient): Task[Long] = run {
    quote {
      query[CoreClient].insert(lift(coreClient))
    }
  }

  def queryAll(): Task[List[CoreClient]] = run {
    quote {
      query[CoreClient]
    }
  }

  def exists(name: String): Task[Boolean] = run {
    quote {
      query[CoreClient].filter(_.name == lift(name))
    }.size
  }.map(_ > 0)

  def update(coreClient: CoreClient): Task[Long] =
    exists(coreClient.name).flatMap {
      case false => insert(coreClient)
      case _     => delete(coreClient.name).flatMap(_ => insert(coreClient))
    }

  // Not Supported by H2 ...
  /*run {
    quote {
      query[CoreClient].insert(lift(coreClient)).onConflictUpdate(_.name)(
        (t, e) => t.ipAddress -> e.ipAddress,
        (t, e) => t.port      -> e.port,
        (t, e) => t.password  -> e.password,
        (t, e) => t.addedBy   -> e.addedBy
      )
    }
  }*/

  def delete(name: String): Task[Long] = run {
    quote {
      query[CoreClient].filter(_.name == lift(name)).delete
    }
  }

  def searchBy(ip: IP, port: Int): OptionT[Task, CoreClient] = OptionT(
    run {
      quote {
        query[CoreClient].filter(c => c.address == lift(ip.toString) && c.port == lift(port))
      }
    }.map(_.headOption)
  )

}

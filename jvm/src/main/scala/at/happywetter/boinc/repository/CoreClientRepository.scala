package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.util.IP
import cats.data.OptionT
import cats.effect.IO
import io.getquill.{H2JdbcContext, SnakeCase}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 08.07.2020
 */
class CoreClientRepository(ctx: H2JdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  def insert(coreClient: CoreClient): IO[Long] = IO.blocking {
    run {
      quote {
        query[CoreClient].insert(lift(coreClient))
      }
    }
  }

  def queryAll(): IO[List[CoreClient]] = IO.blocking {
    run {
      quote {
        query[CoreClient]
      }
    }
  }

  def exists(name: String): IO[Boolean] = IO.blocking {
    run {
      quote {
        query[CoreClient].filter(_.name == lift(name))
      }.size
    } > 0
  }

  def update(coreClient: CoreClient): IO[Long] =
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

  def delete(name: String): IO[Long] = IO.blocking {
    run {
      quote {
        query[CoreClient].filter(_.name == lift(name)).delete
      }
    }
  }

  def searchBy(ip: IP, port: Int): OptionT[IO, CoreClient] = OptionT(
    IO.blocking {
      run {
        quote {
          query[CoreClient].filter(c => c.address == lift(ip.toString) && c.port == lift(port))
        }
      }.headOption
    }
  )

}

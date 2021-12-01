package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.DatabaseDTO.Project
import at.happywetter.boinc.util.quill.ArrayCodec
import cats.effect.IO
import io.getquill.{H2JdbcContext, SnakeCase}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 08.07.2020
 */
class ProjectRepository(ctx: H2JdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  private implicit val arrayEncoder = ArrayCodec.stringArrayEncoder(ctx)
  private implicit val arrayDecoder = ArrayCodec.stringArrayDecoder(ctx)

  def insert(project: Project): IO[Long] = IO.blocking {
    run {
      quote {
        query[Project].insert(lift(project))
      }
    }
  }

  def queryAll(): IO[List[Project]] = IO.blocking {
    run {
      quote {
        query[Project]
      }
    }
  }

}

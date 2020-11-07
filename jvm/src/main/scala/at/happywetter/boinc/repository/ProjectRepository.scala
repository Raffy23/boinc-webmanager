package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.DatabaseDTO.Project
import at.happywetter.boinc.util.quill.ArrayCodec
import io.getquill.{H2MonixJdbcContext, SnakeCase}
import monix.eval.Task

/**
 * Created by: 
 *
 * @author Raphael
 * @version 08.07.2020
 */
class ProjectRepository(ctx: H2MonixJdbcContext[SnakeCase]) {
  import ctx._

  private implicit val arrayEncoder = ArrayCodec.stringArrayEncoder(ctx)
  private implicit val arrayDecoder = ArrayCodec.stringArrayDecoder(ctx)

  def insert(project: Project): Task[Long] = run {
    quote {
      query[Project].insert(lift(project))
    }
  }

  def queryAll(): Task[List[Project]] = run {
    quote {
      query[Project]
    }
  }

}

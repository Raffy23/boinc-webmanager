package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.DatabaseDTO.Project
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

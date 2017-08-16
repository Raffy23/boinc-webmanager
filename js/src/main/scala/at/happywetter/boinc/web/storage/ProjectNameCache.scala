package at.happywetter.boinc.web.storage

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
object ProjectNameCache extends DatabaseProvider {

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  private implicit val objStore: String = "project_name_cache"
  private implicit val storeNames = js.Array("project_name_cache")

  def save(projectUri: String, name: String): Future[Unit] = transaction.map(f => f.add(name, projectUri))

  def get(projectUri: String): Future[Option[String]] = transaction.flatMap(f => f.get(projectUri))

}

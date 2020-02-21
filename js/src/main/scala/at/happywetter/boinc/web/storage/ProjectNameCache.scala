package at.happywetter.boinc.web.storage

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
object ProjectNameCache extends DatabaseProvider {

  private[storage] implicit val objStore: String = "project_name_cache"
  private implicit val storeNames: js.Array[String] = js.Array(objStore)

  def save(projectUri: String, name: String): Future[Unit] =
    transaction(f => f.put(name, projectUri))

  def get(projectUri: String): Future[Option[String]] =
    transactionAsync(f => f.get(projectUri))

  def getAll(urls: List[String]): Future[List[(String, Option[String])]] =
    Future.sequence(urls.map(url => ProjectNameCache.get(url).map(name => (url, name))))

}

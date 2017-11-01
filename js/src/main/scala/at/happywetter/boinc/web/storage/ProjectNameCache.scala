package at.happywetter.boinc.web.storage

import at.happywetter.boinc.web.helper.CompatibilityTester

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
  private implicit val storeNames: js.Array[String] = js.Array("project_name_cache")

  def save(projectUri: String, name: String): Future[Unit] =
    if (CompatibilityTester.isFirefox) firefoxTransaction(_.put(name, projectUri))
    else transaction.map(f => f.put(name, projectUri))

  def get(projectUri: String): Future[Option[String]] =
    if (CompatibilityTester.isFirefox) firefoxTransactionAsync(_.get(projectUri).getData)
    else transaction.flatMap(f => f.get(projectUri))

  def getAll(urls: List[String]): Future[List[(String, Option[String])]] =
    Future.sequence(urls.map(url => ProjectNameCache.get(url).map(name => (url, name))))

}

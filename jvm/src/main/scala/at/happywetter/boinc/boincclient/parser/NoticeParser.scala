package at.happywetter.boinc.boincclient.parser

import scala.jdk.CollectionConverters._

import at.happywetter.boinc.shared.boincrpc.Notice

import org.jsoup.nodes.{Document, Element}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
object NoticeParser:

  def fromDocument(document: Document): List[Notice] =
    document
      .getElementsByTag("notice")
      .iterator()
      .asScala
      .toList
      .map(element =>
        Notice(
          element.getElementsByTag("title").text(),
          element.getElementsByTag("description").text(),
          element.getElementsByTag("create_time").text().toDouble,
          element.getElementsByTag("arrival_time").text().toDouble,
          element.getElementsByTag("is_private").text().toInt == 1,
          element.getElementsByTag("project_name").text(),
          element.getElementsByTag("category").text(),
          extractLink(element),
          element.getElementsByTag("seqno").text().toInt
        )
      )

  private val linkPattern = "<link>(http.*)".r
  private def extractLink(element: Element): String =
    val xmlWay = element.getElementsByTag("link").text()

    if xmlWay.isEmpty then
      linkPattern
        .findFirstIn(element.html())
        .map(_.drop("<link>".length))
        .map(_.replaceAll("&amp;", "&"))
        .getOrElse("")
    else xmlWay

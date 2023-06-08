package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc.Message
import org.jsoup.nodes.Document

import scala.jdk.CollectionConverters._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
object MessageParser:

  def fromDocument(document: Document): List[Message] =
    document
      .getElementsByTag("msg")
      .iterator()
      .asScala
      .toList
      .map(element =>
        Message(
          element.getElementsByTag("project").first().text(),
          element.getElementsByTag("pri").first().text().toInt,
          element.getElementsByTag("seqno").first().text().toLong, {
            val tag = element.getElementsByTag("time").first()
            if tag != null then tag.text().toLong
            else 0
          },
          element.textNodes().asScala.map(_.getWholeText).mkString("\n").trim
        )
      )

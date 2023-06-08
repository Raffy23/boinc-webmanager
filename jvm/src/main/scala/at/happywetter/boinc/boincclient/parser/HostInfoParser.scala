package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc.{CoProcessor, HostInfo}

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object HostInfoParser:
  private def toList(nodeSeq: NodeSeq): List[String] = nodeSeq.text.split(" ").toList
  private def getCoproc(nodeSeq: NodeSeq): List[CoProcessor] = Array[CoProcessor]().toList

  def fromXML(node: NodeSeq): HostInfo =
    HostInfo(
      (node \ "domain_name").text,
      (node \ "ip_addr").text,
      (node \ "host_cpid").text,
      (node \ "p_ncpus").text.toInt,
      (node \ "p_vendor").text,
      (node \ "p_model").text,
      toList(node \ "p_features"),
      (node \ "p_fpops").text.toDouble,
      (node \ "p_iops").text.toDouble,
      (node \ "p_membw").text.toDouble,
      (node \ "m_nbytes").text.toDouble,
      (node \ "m_cache").text.toDouble,
      (node \ "m_swap").text.toDouble,
      (node \ "d_total").text.toDouble,
      (node \ "d_free").text.toDouble,
      (node \ "os_name").text,
      (node \ "os_version").text,
      getCoproc(node \ "coprocs"),
      (node \ "virtualbox_version").toList.headOption.map(n => n.text)
    )

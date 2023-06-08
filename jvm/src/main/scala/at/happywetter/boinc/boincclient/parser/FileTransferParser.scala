package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc.{FileTransfer, FileXfer, PersistentFileXfer}

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object FileTransferParser:

  def fromXML(node: NodeSeq): List[FileTransfer] = (node \ "file_transfer").map(fromNode(_)).toList

  private def fromNode(node: NodeSeq): FileTransfer =
    FileTransfer(
      (node \ "project_url").text,
      (node \ "project_name").text,
      (node \ "name").text,
      (node \ "nbytes").text.toDouble,
      (node \ "status").text.toInt,
      fromPxferNode(node \ "persistent_file_xfer"),
      fromXFerNode(node \ "file_xfer"),
      (node \ "project_backoff").tryToDouble
    )

  private def fromPxferNode(node: NodeSeq): PersistentFileXfer =
    PersistentFileXfer(
      (node \ "num_retries").text.toInt,
      (node \ "first_request_time").text.toDouble,
      (node \ "next_request_time").text.toDouble,
      (node \ "time_so_far").text.toDouble,
      (node \ "last_bytes_xferred").text.toDouble,
      (node \ "is_upload").text.toInt == 1
    )

  private def fromXFerNode(node: NodeSeq): FileXfer =
    if node.isEmpty then FileXfer(0d, 0d, "")
    else
      FileXfer(
        (node \ "bytes_xferred").text.toDouble,
        (node \ "xfer_speed").text.toDouble,
        (node \ "url").text
      )

package at.happywetter.boinc.boincclient.parser

import scala.xml.NodeSeq

import at.happywetter.boinc.shared.boincrpc.AppConfig

object AppConfigParser:
  import AppConfig._

  def fromXML(node: NodeSeq): AppConfig = AppConfig(
    (node \ "app").theSeq
      .map(node =>
        App(
          (node \ "name").text,
          (node \ "max_concurrent").tryToInt,
          (node \ "report_results_immediately").existsNode,
          (node \ "fraction_done_exact").existsNode,
          (node \ "gpu_versions").toOption.map(node =>
            GpuVersions(
              (node \ "gpu_usage").tryToDouble,
              (node \ "cpu_usage").tryToDouble
            )
          )
        )
      )
      .toList,
    (node \ "app_version").theSeq
      .map(node =>
        AppVersion(
          (node \ "app_name").text,
          (node \ "plan_class").text,
          (node \ "avg_cpus").tryToInt,
          (node \ "ngpus").tryToInt,
          (node \ "cmdline").text
        )
      )
      .toList,
    (node \ "project_max_concurrent").toIntOption,
    (node \ "report_results_immediately").existsNode
  )

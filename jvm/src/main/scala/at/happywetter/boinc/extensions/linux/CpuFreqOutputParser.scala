package at.happywetter.boinc.extensions.linux

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.11.2017
  */
object CpuFreqOutputParser:

  private val Pattern = """current CPU frequency: (.*) GHz""".r

  // cpupower frequency-info -f -m
  def parse(output: String): Double =
    Pattern.findFirstMatchIn(output).map(_.group(1).toDouble).getOrElse(0d)

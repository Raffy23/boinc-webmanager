package at.happywetter.boinc.web.chartjs

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.09.2017
  */
object ChartColors {

  lazy val stream: LazyList[String] = colors.to(LazyList) #::: stream

  val colors = List(
    "#CF631D",
    "#CFBC1D",
    "#89CF1D",
    "#30CF1D",
    "#1DCF63",
    "#1DCFBC",
    "#5A79C6",
    "#715AC6",
    "#A75AC6",
    "#C65AAF",
    "#C65A79",
    "#C6715A",
    "#62C65A",
    "#5AC688",
    "#5AC6BE",
    "#5A98C6",
    "#C4C640",
    "#CE62BE",
    "#CE7262"
  )

}

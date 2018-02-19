package at.happywetter.boinc.web.css

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.09.2017
  */
import at.happywetter.boinc.web.pages.boinc.BoincStatisticsLayout

import scalacss.ProdDefaults._
object FloatingMenu extends StyleSheet.Inline {
  import dsl._
  import scala.language.postfixOps

  val root = style(
    float.right,
    marginTop(12 px),
    marginRight(20 px),

    unsafeChild("a")(
      BoincStatisticsLayout.Style.button,

      &.firstChild(
        borderLeft :=! "1px #AAA solid"
      )
    ),
  )

  val active = BoincStatisticsLayout.Style.active

}

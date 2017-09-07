package at.happywetter.boinc.web.css

import scala.language.postfixOps
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object GlobalPageStyle extends StyleSheet.Inline {
  import dsl._

  style(
    unsafeRoot("body")(
      position.absolute,
      margin.`0`,
      padding.`0`,
      fontSize(14 px),
      fontFamily :=! "Roboto, sans-serif",
      height(100 %%),
      width(100 %%),
      backgroundColor(c"#f7f7f7"),
      backgroundSize := "cover"
    ),

    unsafeRoot("html")(
      height(100 %%),
      width(100 %%),
      position.absolute
    ),

    unsafeRoot("main")(
      marginTop(80 px),
      height(100 %%),
      marginBottom(30 px)
    )
  )

}

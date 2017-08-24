package at.happywetter.boinc.web.css

import scalacss.internal.mutable.StyleSheet
import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
import scalacss.DevDefaults._
object TableTheme extends StyleSheet.Inline {
  import dsl._

  val table = style(
    width :=! s"calc(100% - 20px)",
    //maxWidth :=! s"calc(100% - 20px)",
    marginBottom(20 px),
    border :=! "1px solid #DDD",
    borderSpacing.`0`,
    borderCollapse.collapse,


    unsafeChild("thead>tr>th")(
      border :=! "1px solid #DDD",
      borderBottomWidth(2 px),
      padding(10 px),
      textAlign.left,
      fontSize(17 px)
    ),

    unsafeChild("tr>td")(
      verticalAlign.middle,
      border :=! "1px solid #DDD",
      padding(10 px),
      textAlign.left
    ),

    unsafeChild("tbody>tr:nth-child(2n)")(
      backgroundColor(c"#e6e6e6")
    ),

    unsafeChild("tbody>tr>td:last-of-type")(
      padding(1 px, 1 px, 1 px, 10 px),

      unsafeChild("a")(
        textDecoration := "none",
        paddingRight(5 px),
        color(c"#333"),

        &.hover(
          color(c"#428bca")

        ),

        unsafeChild("a > i")(
          fontSize(22 px)
        ),

        unsafeChild("i")(
          fontSize(22 px)
        )
      )
    ),

    unsafeChild("tbody>tr")(
      &.hover(
        backgroundColor(c"#d7e6f4")
      )
    )
  )

  val table_lastrowsmall = style(unsafeChild("tbody>tr>td:last-of-type")(width(6 em)))

}

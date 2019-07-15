package at.happywetter.boinc.web.css

import scalacss.internal.mutable.StyleSheet
import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
import scalacss.ProdDefaults._
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
      textAlign.center,
      fontSize(15 px)
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

  val table_lastrowsmall = style(
    unsafeChild("tbody>tr>td:last-of-type")(
      width(8 em),

      unsafeChild("div")(
          textAlign.center
      ),

      unsafeChild("a > i")(
        width(1 em)
      )
    )
  )

  val vertical_table_text = style(
    width(95 px),
    border.none.important,
    borderBottom :=! "2px #DDD solid",
    whiteSpace.nowrap,

    unsafeChild("div")(
      transform := "rotate(325deg)",
      width(2 px),
      marginBottom(-15 px),
      marginLeft :=! "calc(100% + 1px)",

      unsafeChild("span")(
        borderBottom :=! "1px solid #CCC",
        padding(5 px, 10 px)
      )
    )
  )

  val no_border = style(
    border.none,

    unsafeChild("thead>tr>th")(
      border.none
    ),

  )

}

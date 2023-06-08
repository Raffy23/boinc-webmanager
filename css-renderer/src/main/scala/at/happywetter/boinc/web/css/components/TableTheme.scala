package at.happywetter.boinc.web.css.components

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import at.happywetter.boinc.web.css.components.BasicModalCSS.&

import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object TableTheme extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.components.TableTheme._
  import dsl._

  container.cssName - (
    unsafeChild("> div:first-child")(
      width :=! "calc(100% - 20px)",
      marginBottom(10 px),
      fontWeight.bold
    ),
    unsafeChild("> div:last-child")(
      width :=! "max-content",
      float.right,
      marginRight(15 px),
      marginBottom(20 px),
      unsafeChild("input")(
        width(3 em)
      ),
      unsafeChild("a")(
        userSelect :=! "none",
        cursor.pointer,
        outline.`0`,
        border :=! "1px solid #AAA",
        padding(7 px),
        margin(4 px, 4 px),
        &.hover(
          backgroundColor(c"#B3D0E9")
        )
      )
    )
  )

  table.cssName - (
    width :=! s"calc(100% - 20px)",
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
      ),
      unsafeChild("td[data-type=number]")(
        textAlign.right
      )
    )
  )

  lastRowSmall.cssName -
    unsafeChild("tbody>tr>td:last-of-type")(
      width(8 em),
      unsafeChild("div")(
        textAlign.center
      ),
      unsafeChild("a > i")(
        width(1 em)
      )
    )

  verticalText.cssName - (
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

  noBorder.cssName - (
    border.none,
    unsafeChild("thead>tr>th")(
      border.none
    )
  )

  sortable.cssName - (
    cursor.pointer,
    unsafeChild("i")(
      float.right,
      marginLeft(10 px)
    ),
    unsafeChild("i.fa-sort")(
      color.gray
    )
  )

package at.happywetter.boinc.web.css.pages

import scala.language.postfixOps

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincMessageCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.pages.BoincMessageStyle._

  import dsl._

  dateCol.cssName - (
    whiteSpace.nowrap
  )

  tableRow.cssName - (
    &.attr("data-prio", "2")(
      // fontWeight.bold,
      color(c"#ff1a1a")
    ),
    &.attr("data-prio", "3")(
      fontWeight.bold,
      color(c"#ff1a1a")
    )
  )

  noticeList.cssName - (
    padding.`0`,
    listStyle := "none",
    unsafeChild("li")(
      unsafeChild("h4")(
        fontSize(21 px)
      ),
      unsafeChild("p")(
        lineHeight(1.4923),
        marginTop(-2 px),
        unsafeChild("a")(
          color(c"#0039e6")
        )
      ),
      unsafeChild("small")(
        color(c"#888"),
        unsafeChild("a")(
          color(c"#0039e6"),
          marginLeft(7 px)
        )
      )
    )
  )

  filterBox.cssName - (
    marginBottom(10 px),
    unsafeChild("label")(
      marginLeft(8 px)
    )
  )

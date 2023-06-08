package at.happywetter.boinc.web.css

import AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object PageLayoutStyle extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.components.PageLayoutStyle._
  import dsl._

  heading.cssName - (
    position.fixed,
    top.`0`,
    left.`0`,
    width(100 %%),
    height(50 px),
    paddingLeft(15 px),
    backgroundColor(c"#222"),
    color(c"#F2F2F2"),
    boxShadow := "0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23)",
    zIndex :=! "100",
    media.maxWidth(690 px)(
      height(100 px)
    )
  )

  headerText.cssName - (
    display.inlineBlock,
    fontWeight._300,
    marginTop(10 px),
    marginBottom(10 px),
    fontSize(22 px)
  )

  versionField.cssName - (
    marginLeft(20 px)
  )

  footer.cssName - (
    position.fixed,
    bottom.`0`,
    left.`0`,
    width(100 %%),
    height(35 px),
    backgroundColor(c"#222"),
    color(c"#F2F2F2"),
    textAlign.center,
    fontWeight.lighter,
    fontSize.smaller
  )

  clientContainer.cssName - (
    marginLeft(229 px),
    media.maxWidth(690 px)(
      marginLeft(5 px)
    )
  )

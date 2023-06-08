package at.happywetter.boinc.web.css

import scalacss.defaults.Exports
import scalacss.internal.mutable.Settings

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object AppCSS:

  val CSSDefaults: Exports with Settings =
    AppCSSRegistry.registerCSSNames()
    scalacss.ProdDefaults // devOrProdDefaults

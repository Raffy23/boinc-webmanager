package at.happywetter.boinc.web.helper

import mhtml.{Rx, Var}

import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.02.2018
  */
object RichRx {

  implicit class NowRx[T](rx: Rx[T]) {
    def now: T = extract(rx)
  }

  implicit class NowVar[T](rx: Var[T]) {
    def now: T = extract(rx)
  }

  implicit class FutureRx[R](future: Future[R]) {
    def toRx(default: R): Rx[R] = {
      val rx = Var[R](default)
      future.foreach(async => rx := async)

      rx
    }
  }

  protected def extract[T](rx: Rx[T]): T = {
    var value: Option[T] = None
    rx.impure.run(v => value = Some(v)).cancel

    value.get
  }

}

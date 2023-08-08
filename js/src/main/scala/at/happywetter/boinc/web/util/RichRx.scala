package at.happywetter.boinc.web.util

import scala.concurrent.Future

import mhtml.Rx
import mhtml.Var

/**
  * Created by:
  *
  * @author Raphael
  * @version 06.02.2018
  */
object RichRx:
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  implicit class NowRx[T](private val rx: Rx[T]) extends AnyVal:
    def now: T = extract(rx)

  implicit class NowVar[T](private val rx: Var[T]) extends AnyVal:
    def now: T = extract(rx)

  implicit class FutureRx[R](private val future: Future[R]) extends AnyVal:
    def toRx(default: R): Var[R] =
      val rx = Var[R](default)
      future.foreach(async => rx := async)

      rx

    def toRx(default: R, failure: R): Var[R] =
      val rx = Var[R](default)
      future
        .map(async => rx := async)
        .recover { case _ => rx := failure }

      rx

  @inline protected def extract[T](rx: Rx[T]): T =
    var value: Option[T] = None
    rx.impure.run(v => value = Some(v)).cancel

    value.get

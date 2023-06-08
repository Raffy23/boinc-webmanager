package at.happywetter.boinc.util

import at.happywetter.boinc.util.Observer.Subscriber
import cats.effect.IO
import cats.effect.kernel.{Ref, Resource, Spawn}
import cats.effect.std.Queue

class Observer[A] private (subscribers: Ref[IO, List[Subscriber[A]]], queue: Queue[IO, A]):

  private val backgroundTask =
    queue.take.flatTap { msg =>
      subscribers.get
        .flatMap { subscribers =>
          import cats.implicits._
          subscribers
            .map(_(msg))
            .sequence
        }
    }.foreverM

  def enqueue(event: A): IO[Unit] =
    queue.offer(event)

  def contains(subscriber: Subscriber[A]): IO[Boolean] =
    subscribers.access.map(_._1.contains(subscriber))

  def register(subscriber: Subscriber[A]): IO[Unit] =
    subscribers.update(subscriber :: _)

  def unregister(subscriber: Subscriber[A]): IO[Unit] =
    subscribers.update(_.filterNot(_ == subscriber))

object Observer:

  type Subscriber[A] = A => IO[Unit]

  def unbounded[A]: Resource[IO, Observer[A]] =
    for {
      queue <- Resource.eval(Queue.unbounded[IO, A])
      observer <- Observer.of[A](queue)
    } yield observer

  def of[A](queue: Queue[IO, A]): Resource[IO, Observer[A]] =
    for {
      observer <- Resource.eval(
        for {
          subscribers <- Ref.of[IO, List[Subscriber[A]]](List.empty[Subscriber[A]])
          observer <- IO.pure(new Observer[A](subscribers, queue))
        } yield observer
      )

      _ <- Spawn[IO].background[Nothing](observer.backgroundTask)
    } yield observer

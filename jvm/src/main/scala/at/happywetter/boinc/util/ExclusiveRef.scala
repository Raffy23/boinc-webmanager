package at.happywetter.boinc.util

import cats.effect.IO
import cats.effect.std.Semaphore

object ExclusiveRef {

  def apply[T](initialValue: T): IO[ExclusiveRef[T]] = for {
    lock <- Semaphore[IO](1)
    ref  <- IO.pure(new ExclusiveRef(initialValue, lock))
  } yield ref

}

class ExclusiveRef[T] private(private var value: T, lock: Semaphore[IO]) {

  def update(f: T => T): IO[Unit] =
    lock.permit.use { _ =>
      IO {
        value = f(value)
      }
    }

  def get: IO[T] =
    lock.permit.use { _ =>
      IO.pure(value)
    }

  def set(newValue: T): IO[Unit] =
    lock.permit.use { _ =>
      IO.pure {
        value = newValue
      }
    }

  def modifyF[A](newF: T => IO[(T, A)]): IO[A] =
    lock.permit.use { _ =>
      newF(value).map{ case (newValue, retValue) =>
        value = newValue
        retValue
      }
    }

  def modify[A](f: T => (T, A)): IO[A] =
    lock.permit.use { _ =>
      IO {
        val (newValue, retValue) = f(value)
        value = newValue
        retValue
      }
    }

}

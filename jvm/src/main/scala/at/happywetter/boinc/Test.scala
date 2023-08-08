import cats.effect.IO
import fs2.concurrent.SignallingRef
class Test {
  SignallingRef.of[IO, String]("").map { signal =>
    signal.discrete.evalMap(str => IO {}).compile.drain
  }

}

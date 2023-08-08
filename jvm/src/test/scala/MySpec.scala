import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

// For some reason metals hits the bucket if no tests are defined
class MySpec extends AsyncFlatSpec with AsyncIOSpec with Matchers:

  it should "produce 1" in:
    IO(1).asserting(_ shouldBe 1)

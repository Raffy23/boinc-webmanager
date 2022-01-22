package at.happywetter.boinc.util

import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.boincrpc._
import at.happywetter.boinc.util.PooledBoincClient.{BoincClientParameters, ConnectionException, PoolState}
import cats.data.EitherT
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{IO, Ref, Resource}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by:
  *
  * @author Raphael
  * @version 12.09.2017
  */
object PooledBoincClient {

  case class ConnectionException(e: Throwable) extends RuntimeException(e)

  case class BoincClientParameters(address: String, val port: Int, password: String)

  case class PoolState(lastUsed: Map[BoincClient, Long], finalizer: Map[BoincClient, IO[Unit]], pool: List[BoincClient])

  def apply(poolSize: Int, address: String, port: Int = 31416, password: String): Resource[IO, PooledBoincClient] =
    Resource.make(
      for {
        ref <- ExclusiveRef(PoolState(Map.empty, Map.empty, List.empty))
        client <- IO {
          new PooledBoincClient(BoincClientParameters(address, port, password), ref)
        }
      } yield client
    )(_.close())

}

class PooledBoincClient private(val details: BoincClientParameters, state: ExclusiveRef[PoolState]) extends BoincCoreClient[IO] {

  val deathCounter: Ref[IO, Int] = Ref.unsafe(0)

  private def all = state.get.map(_.lastUsed.keys.toSeq)

  private def takeConnection(): IO[BoincClient] = {
    state.modifyF[Either[Throwable, BoincClient]] { state =>
      state.pool match {
        case connection :: tail => IO.pure {
          (
            PoolState(
              state.lastUsed + (connection -> System.currentTimeMillis()),
              state.finalizer,
              tail
            ),
            Right(connection)
          )
        }

        case Nil    => EitherT(
          BoincClient(details.address, details.port, details.password)
            .allocated
            .map(Right(_))
            .handleError(Left(_))
        ).fold (
          throwable =>
            (
              state,
              Left(throwable)
            )
          ,
          boincClient =>
            (
              PoolState(
                state.lastUsed  + (boincClient._1 -> System.currentTimeMillis()),
                state.finalizer + (boincClient._1 -> boincClient._2),
                List.empty
              ),
              Right(boincClient._1)
            )
        )
      }
    }
    .flatMap(_.fold(IO.raiseError, c => IO.pure(c)))
    .handleErrorWith(cause =>
      deathCounter.update(_ + 1) *>
      IO.raiseError(new RuntimeException(s"Client ${details.address}:${details.port} is unavailable!", cause))
    )
  }

  private def connection[R](extractor: BoincClient => IO[R]): IO[R] = {
    takeConnection().bracketCase(extractor) {
      case (connection, Succeeded(_)) => state.update(old => old.copy(pool = connection :: old.pool))
      case (connection, Canceled())   => state.update(old => old.copy(pool = connection :: old.pool))
      case (connection, Errored(e))   => for {

        _ <- deathCounter.update(_ + 1)
        _ <- closeConnection(connection)
        _ <- IO.raiseError(ConnectionException(e))

      } yield ()
    }
  }

  private def closeConnection(connection: BoincClient): IO[Unit] = for {

    finalizer <- state.modify(state =>
      (
        PoolState(
          state.lastUsed - connection,
          state.finalizer - connection,
          state.pool.filterNot(_ == connection)
        ),
        state.finalizer(connection)
      )
    )

    _ <- finalizer

  } yield ()

  def close(): IO[Unit] = {
    import cats.implicits._

    all
      .flatMap(_
        .map(closeConnection)
        .sequence
      )
      .void
  }

  def isAvailable: IO[Boolean] =
    getHostInfo
      .map(_ => true)
      .handleError(e => false)

  def close(timeout: FiniteDuration): IO[Int] = {
    import cats.implicits._
    val current = System.currentTimeMillis()
    val timeoutMillis = timeout.toMillis

    state.modify { state =>
      val toBeClosed = state.lastUsed.filter {
        case (_, timestamp) => timestamp+timeoutMillis < current
      }.keySet

      val finalizers = state.finalizer
        .filter(entry => toBeClosed.contains(entry._1))
        .values
        .toList

      (
        PoolState(
          state.lastUsed  -- toBeClosed,
          state.finalizer -- toBeClosed,
          state.pool.filterNot(client => toBeClosed.contains(client))
        ),
        (finalizers,  toBeClosed.size)
      )
    }.flatTap(_._1.sequence_)
      .map(_._2)
  }

  def hasOpenConnections: IO[Boolean] = state.get.map(_.pool.nonEmpty)

  override def getTasks(active: Boolean): IO[List[Result]] = connection(_.getTasks(active))

  override def getHostInfo: IO[HostInfo] = connection(_.getHostInfo)

  override def isNetworkAvailable: IO[Boolean] = connection(_.isNetworkAvailable)

  override def getDiskUsage: IO[DiskUsage] = connection(_.getDiskUsage)

  override def getProjects: IO[List[Project]] = connection(_.getProjects)

  override def getState: IO[BoincState] = connection(_.getState)

  override def getFileTransfer: IO[List[FileTransfer]] = connection(_.getFileTransfer)

  override def getCCState: IO[CCState] = connection(_.getCCState)

  override def getStatistics: IO[Statistics] = connection(_.getStatistics)

  override def getGlobalPrefsOverride: IO[GlobalPrefsOverride] = connection(_.getGlobalPrefsOverride)

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): IO[Boolean] =
    connection(_.setGlobalPrefsOverride(globalPrefsOverride))

  override def workunit(project: String, name: String, action: WorkunitAction): IO[Boolean] =
    connection(_.workunit(project, name, action))

  override def project(name: String, action: ProjectAction): IO[Boolean] =
    connection(_.project(name, action))

  override def attachProject(url: String, authenticator: String, name: String): IO[Boolean] =
    connection(_.attachProject(url, authenticator, name))

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setCpu(mode, duration))

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setGpu(mode, duration))

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setNetwork(mode, duration))

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setRun(mode, duration))

  override def getMessages(seqno: Int): IO[List[Message]] = connection(_.getMessages(seqno))

  override def getNotices(seqno: Int): IO[List[Notice]] = connection(_.getNotices(seqno))

  override def readGlobalPrefsOverride: IO[Boolean] = connection(_.readGlobalPrefsOverride)

  override def retryFileTransfer(project: String, file: String): IO[Boolean] =
    connection(_.retryFileTransfer(project, file))

  override def getVersion: IO[BoincVersion] =
    connection(_.getVersion)

  override def getAppConfig(url: String): IO[AppConfig] =
    connection(_.getAppConfig(url))

  override def setAppConfig(url: String, config: AppConfig): IO[Boolean] =
    connection(_.setAppConfig(url, config))

  override def quit(): IO[Unit] =
    connection(_.quit())
}


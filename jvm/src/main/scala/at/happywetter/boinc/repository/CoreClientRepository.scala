package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import cats.effect.IO
import com.comcast.ip4s.IpAddress
import doobie.{Transactor, Write}
import doobie.implicits.toSqlInterpolator
import doobie.implicits._
import doobie.h2.implicits._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 08.07.2020
 */
class CoreClientRepository(xa: Transactor[IO]) {

  private implicit val CoreClientWrite: Write[CoreClient] = Write[(String, String, Int, String, String)].contramap( coreClient =>
    (coreClient.name, coreClient.address, coreClient.port, coreClient.password, coreClient.addedBy)
  )

  private implicit val IPAddressWrite: Write[IpAddress] = Write[String].contramap(ip => ip.toInetAddress.getHostAddress)

  def insert(coreClient: CoreClient): IO[Int] =
    sql"""INSERT INTO core_client (name, address, port, password, added_by) VALUES ($coreClient)"""
      .update
      .run
      .transact(xa)

  def queryAll(): IO[List[CoreClient]] =
    sql"""SELECT * FROM core_client"""
      .query[CoreClient]
      .to[List]
      .transact(xa)

  def exists(name: String): IO[Boolean] =
    sql"""SELECT COUNT(*) FROM core_client WHERE name = $name"""
      .query[Int]
      .unique
      .transact(xa)
      .map(_ == 1)

  def update(coreClient: CoreClient): IO[Int] =
    exists(coreClient.name).flatMap {
      case false => insert(coreClient)
      case true  => delete(coreClient.name).flatMap(_ =>
        insert(coreClient)
      )
    }

  def delete(name: String): IO[Int] =
    sql"""DELETE FROM core_client WHERE name = $name"""
      .update
      .run
      .transact(xa)

  def searchBy(ip: IpAddress, port: Int): IO[List[CoreClient]] =
    sql"""SELECT * FROM core_client WHERE address = $ip AND port = $port"""
      .query[CoreClient]
      .to[List]
      .transact(xa)

}

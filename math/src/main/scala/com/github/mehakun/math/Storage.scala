package com.github.mehakun.math

import cats.{Applicative}
import cats.data.EitherT
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID

trait Storage[F[_]] {
  /// works as update entry or add new one
  def store(id: FUUID, result: Option[Seq[Int]]): F[Unit]

  def keys: F[Seq[FUUID]]

  def getResult(id: FUUID): EitherT[F, Storage.StorageError, Seq[Int]]
}

object Storage {
  sealed abstract class StorageError extends Product with Serializable
  object StorageError {
    final case object KeyNotFound extends StorageError
    final case object ValueNotFound extends StorageError
  }

  def apply[F[_]](implicit ev: Storage[F]): Storage[F] = ev

  def inMemoryStorage[F[_]: Applicative](
    map: java.util.concurrent.ConcurrentMap[FUUID, Option[Seq[Int]]]
  ): Storage[F] = new Storage[F] {
    final override def store(id: FUUID, result: Option[Seq[Int]]): F[Unit] =
      // execeptions from map are really exceptional dunno how to handle them
      (map.put(id, result): Unit).pure[F]

    final override def keys: F[Seq[FUUID]] = {
      import scala.jdk.CollectionConverters._
      map.keySet().asScala.toSeq.pure[F]
    }

    final override def getResult(
      id: FUUID
    ): EitherT[F, StorageError, Seq[Int]] =
      EitherT.fromEither {
        Option(map.get(id)) match {
          case Some(Some(s)) => Right(s)
          case Some(None)    => Left(StorageError.ValueNotFound)
          case None          => Left(StorageError.KeyNotFound)
        }
      }
  }
}

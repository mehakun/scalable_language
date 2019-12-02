package com.github.mehakun.math

import cats.Functor
import cats.data.EitherT
import cats.effect.{IO, LiftIO}
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

  def inMemoryStorage[F[_]: Functor: LiftIO](
    map: java.util.concurrent.ConcurrentMap[FUUID, Option[Seq[Int]]]
  ): Storage[F] = new Storage[F] {
    final override def store(id: FUUID, result: Option[Seq[Int]]): F[Unit] =
      // exceptions from map are really exceptional dunno how to handle them
      IO(map.put(id, result): Unit).to[F]

    final override def keys: F[Seq[FUUID]] = {
      import scala.jdk.CollectionConverters._
      IO(map.keySet().asScala.toSeq).to[F]
    }

    final override def getResult(
      id: FUUID
    ): EitherT[F, StorageError, Seq[Int]] =
      EitherT(IO(Option(map.get(id))).to[F].map {
        case Some(Some(s)) => Right(s)
        case Some(None)    => Left(StorageError.ValueNotFound)
        case None          => Left(StorageError.KeyNotFound)
      })
  }
}

package com.github.mehakun.math

import cats.data.EitherT
import cats.effect.{Async, Concurrent, Sync}
import cats.Monad
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
import fs2.Stream

import scala.concurrent.ExecutionContext

trait Calculator[F[_]] {
  def calculate(input: Calculator.InputArrays,
                operator: Calculator.Operator,
                concurrencyLevel: Int): F[FUUID]

  // does not wait completion of calculation just returns current state
  def getResult(id: FUUID): EitherT[F, Storage.StorageError, Calculator.Result]

  // TODO
  //def calculateSkipped: F[Unit]
}

object Calculator {
  type InputArrays = List[List[Int]]
  type Result = List[Int]
  type Operator = (Int, Int) => Int

  def apply[F[_]](implicit ev: Calculator[F]): Calculator[F] = ev

  def simpleCalculator[F[_]: Sync: Monad: Concurrent: Async](
    storage: Storage[F]
  ): Calculator[F] =
    new Calculator[F] {
      private def reduceInput(input: InputArrays,
                              operator: Operator,
                              concurrencyLevel: Int): Stream[F, Result] = {
        import cats.implicits._
        Stream
          .emit(input)
          .covary[F]
          .parEvalMap(concurrencyLevel)(_.map(_.reduce(operator)).pure[F])
      }

      final override def calculate(input: InputArrays,
                                   operator: Operator,
                                   concurrencyLevel: Int): F[FUUID] =
        for {
          id <- FUUID.randomFUUID[F]
          _ <- storage.store(id, None)
          _ <- Concurrent[F].start(
            Sync[F]
              .delay {
                reduceInput(input, operator, concurrencyLevel)
                  .evalMap(r => storage.store(id, Some(r)))
              }
              .flatMap(_.compile.drain)
          )
        } yield id

      final override def getResult(
        id: FUUID
      ): EitherT[F, Storage.StorageError, Result] =
        storage.getResult(id).map(_.toList)
    }
}

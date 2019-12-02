package com.github.mehakun.math

import cats.Functor
import cats.data.{Kleisli, OptionT}
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import io.chrisdavenport.fuuid.FUUID
import org.http4s.{Request, Response}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object MathServer {
  final class KleisliResponseOps[F[_]: Functor, A](
    self: Kleisli[OptionT[F, ?], A, Response[F]]
  ) {
    def orElseResponseWith(resp: Response[F]): Kleisli[F, A, Response[F]] =
      Kleisli(a => self.run(a).getOrElse(resp))
  }

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F],
                                     C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).stream
      storageAlg = Storage.inMemoryStorage[F](
        new java.util.concurrent.ConcurrentHashMap[FUUID, Option[Seq[Int]]]()
      )

      calculatorAlg = Calculator.simpleCalculator[F](storageAlg)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      coreRoutes = MathRoutes.jobReadStateRoutes[F](calculatorAlg) <+>
        MathRoutes.jobWriteStateRoutes[F](calculatorAlg)

      httpApp = Kleisli(
        (a: Request[F]) =>
          coreRoutes.run(a).getOrElseF(MathRoutes.createBadRequestResponse[F])
      )
      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}

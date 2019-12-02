package com.github.mehakun.math

import cats.{Applicative, Functor}
import cats.effect.Sync
import cats.implicits._
import com.github.mehakun.math.Calculator.InputArrays
import com.github.mehakun.math.Storage.StorageError
import io.chrisdavenport.fuuid.http4s.FUUIDVar
import io.chrisdavenport.fuuid.FUUID
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.{EntityDecoder, HttpRoutes, QueryParamDecoder, Response}
import org.http4s.dsl.Http4sDsl

object MathRoutes {

  def jobWriteStateRoutes[F[_]: Sync: Applicative](
    calculator: Calculator[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    implicit val parallelLevelQueryParamDecoder
      : QueryParamDecoder[Option[Int]] =
      QueryParamDecoder[Int].map {
        case lvl if 1 until 11 contains lvl => Some(lvl)
        case _                              => None
      }

    final object ParallelLevelQueryParamMatcher
        extends QueryParamDecoderMatcher[Option[Int]]("parLevel")

    implicit val operationQueryParamDecoder
      : QueryParamDecoder[Option[Calculator.Operator]] =
      QueryParamDecoder[Char].map {
        case '+' => Some(_ + _)
        case '-' => Some(_ - _)
        case '/' => Some(_ / _)
        case '*' =>
          Some(_ * _)
        case _ =>
          None
      }
    final object OperationQueryParamMatcher
        extends QueryParamDecoderMatcher[Option[Calculator.Operator]]("op")

    def collectQueries(
      lvlOpt: Option[Int],
      opOpt: Option[Calculator.Operator]
    ): Option[(Int, Calculator.Operator)] =
      for {
        lvl <- lvlOpt
        op <- opOpt
      } yield (lvl, op)

    implicit val decoder: EntityDecoder[F, InputArrays] =
      jsonOf[F, Calculator.InputArrays]

    HttpRoutes.of[F] {
      case req @ POST -> Root / "jobs" :? ParallelLevelQueryParamMatcher(level) +& OperationQueryParamMatcher(
            op
          ) =>
        for {
          input <- req.as[Calculator.InputArrays]
          resp <- collectQueries(level, op)
            .map {
              case (validatedLevel, validatedOp) =>
                calculator
                  .calculate(input, validatedOp, validatedLevel)
                  .flatMap((a: FUUID) => Ok(Protocol.createIdJson(a)))
            }
            .getOrElse(BadRequest(Protocol.BAD_REQUEST_BODY))
        } yield resp
    }
  }

  def jobReadStateRoutes[F[_]: Sync: Functor](
    calculator: Calculator[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "jobs" / FUUIDVar(id) =>
        calculator
          .getResult(id)
          .toOption
          .map(s => Ok(s.asJson.noSpaces))
          .getOrElse(NotFound(Protocol.NOT_FOUND_BODY))
          .flatten

      case GET -> Root / "jobs" / FUUIDVar(id) / "status" =>
        calculator
          .getResult(id)
          .fold(
            {
              case StorageError.KeyNotFound => Option.empty[String]
              case StorageError.ValueNotFound =>
                Option(Protocol.createStatusJson("PENDING").noSpaces)
            },
            _ => Option(Protocol.createStatusJson("COMPLETED").noSpaces)
          )
          .flatMap {
            case Some(s) => Ok(s)
            case None    => NotFound(Protocol.NOT_FOUND_BODY)
          }
    }
  }

  def createBadRequestResponse[F[_]: Applicative]: F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    BadRequest(Protocol.BAD_REQUEST_BODY)
  }
}

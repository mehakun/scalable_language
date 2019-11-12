package com.github.mehakun.math

import cats.effect.{ContextShift, IO}
import io.chrisdavenport.fuuid.FUUID
import io.circe.Json
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.implicits._
import org.specs2.matcher.MatchResult

import concurrent.ExecutionContext.global

class CalculatorSpec extends org.specs2.mutable.Specification {

  "Post job" >> {
    "return 200" >> {
      uriReturns200()
    }
    "return uuid" >> {
      uriReturnsUUID()
    }
    "status PENDING" >> {
      uriStatusPending()
    }
  }

  private[this] val retPostJob: (Response[IO], Response[IO]) = {
    val postJob =
      Request[IO](Method.POST, uri"/jobs?parLevel=1&op=*")
        .withEntity("[[1,2,3,4],[1,2,3]]")
    val getJob = Request[IO](Method.GET, uri"/jobs?parLevel=1&op=*")
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    val storage = Storage.inMemoryStorage[IO](
      new java.util.concurrent.ConcurrentHashMap[FUUID, Option[Seq[Int]]]()
    )
    val calculator = Calculator.simpleCalculator[IO](storage)
    val write = MathRoutes
      .jobWriteStateRoutes(calculator)
      .orNotFound(postJob)
      .unsafeRunSync()

    implicit val decoder: EntityDecoder[IO, Json] =
      jsonOf[IO, Json]
    val idString = write
      .as[Json]
      .map(_.hcursor.downField("id").as[String])
      .unsafeRunSync()
      .fold(f => "failure", s => s)

    val statusUri = Uri
      .fromString(s"/jobs/${idString}/status")
      .getOrElse(uri"failure")

    (
      write,
      MathRoutes
        .jobReadStateRoutes(calculator)
        .orNotFound(Request[IO](Method.GET, statusUri))
        .unsafeRunSync()
    )
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retPostJob._1.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsUUID(): MatchResult[String] =
    retPostJob._1.as[String].unsafeRunSync() must beMatching(
      """\{"id":"([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}){1}"\}""".r
    )

  private[this] def uriStatusPending(): MatchResult[String] =
    retPostJob._2.as[String].unsafeRunSync() must beMatching(
      """\{"status":"PENDING"\}"""
    )
}

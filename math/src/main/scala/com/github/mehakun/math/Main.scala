package com.github.mehakun.math

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  def run(args: List[String]) =
    MathServer.stream[IO].compile.drain.as(ExitCode.Success)
}
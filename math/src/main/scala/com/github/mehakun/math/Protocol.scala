package com.github.mehakun.math

import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.Json
import io.circe.syntax._

object Protocol {
  final val NOT_FOUND_BODY = "{\"error_code\": \"not_found\"}"
  final val BAD_REQUEST_BODY = "{\"error_code\": \"bad_request\"}"

  final def createStatusJson(status: String): Json =
    Json
      .fromFields(List(("status", Json.fromString(status))))

  final def createIdJson(id: FUUID): Json =
    Json
      .fromFields(List(("id", id.asJson)))
}

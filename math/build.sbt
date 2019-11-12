val Http4sVersion = "0.21.0-M5"
val CirceVersion = "0.12.2"
val Specs2Version = "4.8.0"
val LogbackVersion = "1.2.3"
val FUUIDVersion = "0.3.0-M3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.github.mehakun",
    name := "math",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "org.specs2"      %% "specs2-core"         % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "io.chrisdavenport" %% "fuuid" % FUUIDVersion,
      "io.chrisdavenport" %% "fuuid-circe"  % FUUIDVersion,
      "io.chrisdavenport" %% "fuuid-http4s" % FUUIDVersion,
      "io.chrisdavenport" %% "fuuid-doobie" % FUUIDVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
)

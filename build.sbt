import sbt.Keys._
import sbt._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / organization     := "com.evidentid"
ThisBuild / organizationName := "Evident ID, Inc."
ThisBuild / conflictManager  := ConflictManager.default

// Keep the number of ITs with database connections low enough to not create
// more connections than the Postgres docker instance allows.
Global / concurrentRestrictions += Tags.limit(Tags.Test, 8)

Global / excludeLintKeys ++= Set(configuration)

def atTestFilter = (_: String).matches("com.evidentid..+.test.acceptance.*")
def itTestFilter = (_: String).startsWith("com.evidentid.application.test.integration.")

lazy val AcceptanceTest = config("at") extend IntegrationTest
AcceptanceTest / test              := ((AcceptanceTest / test) dependsOn (IntegrationTest / test)).value
AcceptanceTest / parallelExecution := true
AcceptanceTest / testOptions       := Seq(Tests.Filter(atTestFilter), Tests.Argument("-oD"))
inConfig(AcceptanceTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)

IntegrationTest / test              := ((IntegrationTest / test) dependsOn (Test / test)).value
IntegrationTest / parallelExecution := true
IntegrationTest / testOptions       := Seq(Tests.Filter(itTestFilter), Tests.Argument("-oD"))
inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)

assembly / test := {}

Compile / assembly / artifact := {
  val art = (Compile / assembly / artifact).value
  art.withClassifier(Some("assembly"))
}

addArtifact(Compile / assembly / artifact, assembly)

// akka to 2.6.x and akka http 10.2.x are the latest versions on apache license
lazy val akkaVersion = "2.6.20"
lazy val akkaHttpVersion = "10.2.10"
lazy val catsVersion = "2.7.0"
lazy val flywayVersion = "9.8.2"
lazy val logbackVersion = "1.2.11"
lazy val logstashLogbackVersion = "7.1.1"
lazy val postgresqlVersion = "42.5.0"
lazy val slickVersion = "3.4.1"
lazy val slickPgVersion = "0.21.0"
lazy val tapirVersion = "1.2.3"
lazy val circeVersion = "0.14.3"
lazy val sttpVersion = "3.9.1"

lazy val scalaTestVersion = "3.2.12"
// Boo for insane versioning. Make sure this stays in sync with the main scalatest deps.
lazy val autofix = "org.scalatest" %% "autofix" % "3.2.0.0"
lazy val scalaMockVersion = "5.2.0"

lazy val failOnWarningOptions = sys.env.get("ALLOW_WARNINGS").fold(Seq("-Xfatal-warnings"))(_ => Seq.empty[String])

lazy val root = (project in file("."))
  .configs(IntegrationTest, AcceptanceTest)
  .settings(
    Defaults.itSettings,
    inConfig(AcceptanceTest)(Defaults.testTasks),
    scalacOptions := Seq(
      "-feature",
      "-unchecked",
      "-deprecation",
      "-encoding",
      "utf8",
      "-Xlint",
      "-Yrangepos",
      "-Wdead-code",
      "-Wunused:imports,patvars,privates,locals,explicits",
      "-Xlint:infer-any",
      "-Xlint:private-shadow",
      "-Xlint:type-parameter-shadow",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-unit",
      // Disable multiarg infix warning for byname implicits for Akka Http
      "-Wconf:cat=lint-multiarg-infix:s,cat=lint-byname-implicit:s"
    ) ++ failOnWarningOptions,
    name := "eid-scala-app",
    credentials ++= {
      import BuildPaths.{getGlobalBase, getGlobalSettingsDirectory}
      val state = Keys.state.value
      val defaultCredentialFile =
        getGlobalSettingsDirectory(state, getGlobalBase(state)) / ".credentials"
      val defaultCredentials =
        if (defaultCredentialFile.exists())
          Some(Credentials(defaultCredentialFile))
        else None

      val envCredentials = sys.env.get("SBT_CREDENTIALS").flatMap { path =>
        val f = file(path)
        if (f.exists()) Some(Credentials(f)) else None
      }

      Seq(defaultCredentials, envCredentials).flatten
    },
    mergeSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka"             %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.slick"            %% "slick"                    % slickVersion,
      "com.typesafe.slick"            %% "slick-hikaricp"           % slickVersion,
      "com.github.tminglei"           %% "slick-pg"                 % slickPgVersion,
      "com.github.tminglei"           %% "slick-pg_circe-json"      % slickPgVersion,
      "ch.qos.logback"                 % "logback-classic"          % logbackVersion,
      "net.logstash.logback"           % "logstash-logback-encoder" % logstashLogbackVersion,
      "org.flywaydb"                   % "flyway-core"              % flywayVersion,
      "org.postgresql"                 % "postgresql"               % postgresqlVersion,
      "com.softwaremill.sttp.tapir"   %% "tapir-core"               % tapirVersion,
      "com.softwaremill.sttp.tapir"   %% "tapir-akka-http-server"   % tapirVersion,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"         % tapirVersion,
      "com.softwaremill.sttp.tapir"   %% "tapir-openapi-docs"       % tapirVersion,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"       % "0.3.1",
      "io.circe"                      %% "circe-core"               % circeVersion,
      "io.circe"                      %% "circe-generic"            % circeVersion,
      "io.circe"                      %% "circe-generic-extras"     % circeVersion,
      "de.heikoseeberger"             %% "akka-http-circe"          % "1.39.2",
      "com.softwaremill.sttp.client3" %% "core"                     % sttpVersion,
      "com.softwaremill.sttp.client3" %% "akka-http-backend"        % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe"                    % sttpVersion,
      "org.typelevel"                 %% "cats-core"                % catsVersion,
      "com.typesafe.akka"             %% "akka-actor-testkit-typed" % akkaVersion        % "test,it",
      "com.typesafe.akka"             %% "akka-http-testkit"        % akkaHttpVersion    % "test,it",
      "org.scalatest"                 %% "scalatest"                % scalaTestVersion   % "test,it",
      "org.scalamock"                 %% "scalamock"                % scalaMockVersion   % "test,it",
      "org.scala-lang"                 % "scala-reflect"            % scalaVersion.value % Provided,
    ),
    dependencyOverrides ++= Seq(
      "org.scala-lang.modules" %% "scala-collection-compat"  % "2.0.0", // Induced by Slick 3.3.2
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"
    )
  )

resolvers ++= Seq("jitpack".at("https://jitpack.io"), Resolver.jcenterRepo, Resolver.typesafeRepo("releases"))

lazy val mergeSettings = assembly / assemblyMergeStrategy := {
  case PathList("module-info.class")                            => MergeStrategy.discard
  case x if x.endsWith("/module-info.class")                    => MergeStrategy.discard
  case x if x.contains("javax/annotation/")                     => MergeStrategy.first
  case x if x.contains("META-INF/io.netty.versions.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

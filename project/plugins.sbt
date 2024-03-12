// Copyright (C) 2020 EvidentID, Inc.

ThisBuild / conflictManager := ConflictManager.default

ThisBuild / dependencyOverrides ++= Seq(
  "com.google.protobuf"     % "protobuf-java"      % "3.7.0",
  "com.squareup.okhttp3"    % "okhttp"             % "3.14.2",
  "org.scala-sbt"           % "launcher-interface" % "1.1.3",
  "org.scala-lang.modules" %% "scala-xml"          % "1.2.0",
  "org.slf4j"               % "slf4j-api"          % "1.7.26"
)

addSbtPlugin("com.eed3si9n"  % "sbt-assembly" % "1.1.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")

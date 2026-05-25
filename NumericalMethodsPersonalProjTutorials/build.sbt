val scala3Version = "3.8.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "NumericalMethodsPersonalProjTutorials",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalanlp" %% "breeze" % "2.1.0",
      "org.scalameta" %% "munit" % "1.2.4" % Test
    )
  )

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "CrankNicholsonFiniteDiffScheme",
    libraryDependencies  ++= Seq(
      // Last stable release
      "org.scalanlp" %% "breeze" % "2.1.0",

      // The visualization library is distributed separately as well.
      // It depends on LGPL code
      "org.scalanlp" %% "breeze-viz" % "2.1.0"
    )
  )
import scala.concurrent.duration._

val commonSettings =
  Seq(
    organization := "net.iakovlev",
    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.11.11", "2.12.3"),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    libraryDependencies ++= Seq(compilerPlugin("io.tryp" %% "splain" % "0.2.4"))
  )

lazy val core = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "dynamo-generic-core",
    //scalacOptions ++= Seq("-Xlog-implicits"),
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.2",
      "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
      "org.typelevel" %% "cats" % Versions.cats)
  )

lazy val aws_sdk_bindings =
  (project in file("aws-sdk-bindings"))
    .settings(commonSettings)
    .dependsOn(core)
    .configs(IntegrationTest)
    .settings(
      inConfig(IntegrationTest)(baseDynamoDBSettings),
      Defaults.itSettings,
      name := "Dynamo Java SDK bindings",
      libraryDependencies ++= Seq(
        "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.171",
        "org.typelevel" %% "cats" % Versions.cats,
        "org.specs2" %% "specs2-core" % Versions.specs2 % "test, it"),
      startDynamoDBLocal in IntegrationTest := startDynamoDBLocal
        .dependsOn(compile in IntegrationTest)
        .value,
      test in IntegrationTest := (test in IntegrationTest)
        .dependsOn(startDynamoDBLocal)
        .value,
      testOnly in IntegrationTest := (testOnly in IntegrationTest)
        .dependsOn(startDynamoDBLocal)
        .evaluated,
      testOptions in IntegrationTest += dynamoDBLocalTestCleanup.value,
      dynamoDBLocalDownloadIfOlderThan := 100.days
    )

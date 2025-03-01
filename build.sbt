////////////////////////////////////////////////////////////////////////////////////
// Common Stuff
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import org.apache.commons.io.FileUtils

lazy val root = project
  .in(file("."))
  .aggregate(server, commonJS, commonJVM, webclient)
  .settings(
    publish := {},
    publishLocal := {},
    Global / onChangedBuildSource := ReloadOnSourceChanges,
  )

lazy val buildInfoSettings =
  Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "x.web"
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

ThisBuild / name := "full-scala-stack"
ThisBuild /organization := "net.leibman"
ThisBuild /scalaVersion := "2.12.10"

////////////////////////////////////////////////////////////////////////////////////
// common (i.e. model)
lazy val common =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("common"))
    .jvmSettings(
        libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "0.6.31" % "provided"
    )
    .jsSettings(
    // Add JS-specific settings here
    )

lazy val commonJVM = common.jvm
lazy val commonJS = common.js

////////////////////////////////////////////////////////////////////////////////////
// Server
lazy val akkaVersion = "2.6.1"
lazy val akkaHttpVersion = "10.1.11"
lazy val slickVersion = "3.3.2"
lazy val zioVersion = "1.0.0-RC17"

lazy val start = TaskKey[Unit]("start")

lazy val dist = TaskKey[File]("dist")

lazy val debugDist = TaskKey[File]("debugDist")

lazy val server = project
  .in(file("server"))
  .configs(IntegrationTest)
  .dependsOn(commonJVM)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion withSources(),
      "dev.zio" %% "zio-macros-core" % "0.6.0" withSources(),

      "com.typesafe.slick" %% "slick" % slickVersion withSources(),
      "com.typesafe.slick" %% "slick-codegen" % slickVersion withSources(),
      "mysql" % "mysql-connector-java" % "8.0.18",

      "com.github.daddykotex" %% "courier" % "2.0.0" withSources(),

      "com.github.pathikrit" %% "better-files" % "3.8.0" withSources(),

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "com.lihaoyi" %% "upickle" % "0.8.0" withSources(),
      "de.heikoseeberger" %% "akka-http-upickle" % "1.29.1" withSources(),

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "it,test",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % "it,test",
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "it,test",
      "org.scalatest" %% "scalatest" % "3.1.0" % "it,test",
      "dev.zio" %% "zio-test"     % zioVersion % "it, test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "it, test"
    ),
    testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )

////////////////////////////////////////////////////////////////////////////////////
// Web client
lazy val webclient = project
  .in(file("webclient"))
  .dependsOn(commonJS)
  .enablePlugins(ScalaJSPlugin, ScalajsReactTypedPlugin, AutomateHeaderPlugin, GitVersioning, BuildInfoPlugin)
  .configure(bundlerSettings)
  .settings(gitSettings, buildInfoSettings)
  .settings(
    debugDist := {

      val assets = (ThisBuild / baseDirectory).value / "webclient" / "src" / "main" / "web"

      val artifacts = (Compile / fastOptJS / webpack).value
      val artifactFolder = (Compile / fastOptJS / crossTarget).value
      val debugFolder = (ThisBuild / baseDirectory).value / "debugDist"

      debugFolder.mkdirs()
      FileUtils.copyDirectory(assets, debugFolder, true)
      artifacts.foreach { artifact =>
        val target = artifact.data.relativeTo(artifactFolder) match {
          case None => debugFolder / artifact.data.name
          case Some(relFile) => debugFolder / relFile.toString
        }

        println(s"Trying to copy ${artifact.data.toPath} to ${target.toPath}")
        Files.copy(artifact.data.toPath, target.toPath, REPLACE_EXISTING)
      }

      debugFolder
    },
    dist := {
      val assets = (ThisBuild / baseDirectory).value / "webclient" / "src" / "main" / "web"

      val artifacts = (Compile / fullOptJS / webpack).value
      val artifactFolder = (Compile / fullOptJS / crossTarget).value
      val distFolder = (ThisBuild / baseDirectory).value / "dist"

      distFolder.mkdirs()
      FileUtils.copyDirectory(assets, distFolder, true)
      artifacts.foreach { artifact =>
        val target = artifact.data.relativeTo(artifactFolder) match {
          case None => distFolder / artifact.data.name
          case Some(relFile) => distFolder / relFile.toString
        }

        println(s"Trying to copy ${artifact.data.toPath} to ${target.toPath}")
        Files.copy(artifact.data.toPath, target.toPath, REPLACE_EXISTING)
      }

      distFolder
    },
    resolvers += Resolver.bintrayRepo("oyvindberg", "ScalajsReactTyped"),
    libraryDependencies ++= Seq(
      ScalajsReactTyped.S.`semantic-ui-react`,
      ScalajsReactTyped.S.`stardust-ui__react-component-ref`,
      "commons-io" % "commons-io" % "2.6" withSources(),
      "ru.pavkin" %%% "scala-js-momentjs" % "0.10.0" withSources(),
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC3" withSources(),
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0-RC3_2019a" withSources(),
      "org.scala-js" %%% "scalajs-dom" % "0.9.7" withSources(),
      "com.olvind" %%% "scalablytyped-runtime" % "2.1.0",
      "com.github.japgolly.scalajs-react" %%% "core" % "1.5.0-RC2" withSources(),
      "com.github.japgolly.scalajs-react" %%% "extra" % "1.5.0-RC2" withSources(),
      "com.lihaoyi" %%% "upickle" % "0.8.0" withSources(),
      "com.lihaoyi" %%% "scalatags" % "0.7.0" withSources(),
      "com.github.japgolly.scalacss" %%% "core" % "0.6.0-RC1" withSources(),
      "com.github.japgolly.scalacss" %%% "ext-react" % "0.6.0-RC1" withSources(),
      "com.lihaoyi" %% "upickle" % "0.8.0" % "test" withSources(),
      "com.github.pathikrit" %% "better-files" % "3.8.0",
      "org.scalatest" %% "scalatest" % "3.1.0" % "test" withSources(),
    ),
    organization := "net.leibman",
    organizationName := "Roberto Leibman",
    startYear := Some(2019),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-P:scalajs:sjsDefinedByDefault",
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    webpackDevServerPort := 8009,
  )

lazy val bundlerSettings: Project => Project =
  _.enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      /* disabled because it somehow triggers many warnings */
      emitSourceMaps := false,
      scalaJSModuleKind := ModuleKind.CommonJSModule,
      /* Specify current versions and modes */
      startWebpackDevServer / version := "3.1.10",
      webpack / version := "4.28.3",
      Compile / fastOptJS / webpackExtraArgs += "--mode=development",
      Compile / fullOptJS / webpackExtraArgs += "--mode=production",
      Compile / fastOptJS / webpackDevServerExtraArgs += "--mode=development",
      Compile / fullOptJS / webpackDevServerExtraArgs += "--mode=production",
      useYarn := false,
      //      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv,
      fork in run := true,
      scalaJSStage in Global := FastOptStage,
      scalaJSUseMainModuleInitializer in Compile := true,
      scalaJSUseMainModuleInitializer in Test := false,
      skip in packageJSDependencies := false,
      artifactPath
        .in(Compile, fastOptJS) := ((crossTarget in(Compile, fastOptJS)).value /
        ((moduleName in fastOptJS).value + "-opt.js")),
      artifactPath
        .in(Compile, fullOptJS) := ((crossTarget in(Compile, fullOptJS)).value /
        ((moduleName in fullOptJS).value + "-opt.js")),
      webpackEmitSourceMaps := true,
      Compile / npmDependencies ++= Seq(
        //        "jsdom"-> "^15.0.0",
        "react-dom" -> "16.9",
        "@types/react-dom" -> "16.9.1",
        "react" -> "16.9",
        "@types/react" -> "16.9.5",
        "semantic-ui-react" -> "0.88.1"
      ),
      npmDevDependencies.in(Compile) := Seq(
        //        "jsdom"-> "^15.0.0",
        "style-loader" -> "0.23.1",
        "css-loader" -> "2.1.0",
        "sass-loader" -> "7.1.0",
        "compression-webpack-plugin" -> "2.0.0",
        "file-loader" -> "3.0.1",
        "gulp-decompress" -> "2.0.2",
        "image-webpack-loader" -> "4.6.0",
        "imagemin" -> "6.1.0",
        "less" -> "3.9.0",
        "less-loader" -> "4.1.0",
        "lodash" -> "4.17.11",
        "node-libs-browser" -> "2.1.0",
        "react-hot-loader" -> "4.6.3",
        "url-loader" -> "1.1.2",
        "expose-loader" -> "0.7.5",
        "webpack" -> "4.28.3",
        "webpack-merge" -> "4.2.2"
      )
    )

////////////////////////////////////////////////////////////////////////////////////
// TODO: mobile client


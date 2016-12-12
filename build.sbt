enablePlugins(ScalaJSPlugin)
import sbt.Keys._

val projectName  = "scalajs-webgl-experiments"
val scalaV       = "2.11.6"
val org          = "com.shafer"

lazy val root = project.in(file(".")).
  aggregate(webglExperimentsJS, webglExperimentsJVM).
  settings()

val sharedSettings = Seq(
  scalaVersion := scalaV,
  version      := "0.1-SNAPSHOT",
  organization := org
)

lazy val sharedLibs = Seq()

lazy val webglExperiments = crossProject.in(file("."))
  .settings(
    mainClass in Compile := Some("ExampleAppTextures"),
    name := projectName,
    organization := org,
    scalaVersion := scalaV,
    libraryDependencies ++= sharedLibs ++ Seq() // Shared within cross-project
  ).
  jvmSettings(
    libraryDependencies ++= Seq() // JVM Only
  ).
  jsSettings(
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    jsDependencies ++= Seq(), // Web Jars
    libraryDependencies ++= Seq("org.scala-js" %%% "scalajs-dom" % "0.9.0") // JS Only
  )

lazy val shared = Project(s"$projectName-shared", file("shared"))
   .settings( sharedSettings ++ Seq(
        libraryDependencies ++= sharedLibs ++ Seq()
    )
   )


lazy val webglExperimentsJVM = webglExperiments.jvm
lazy val webglExperimentsJS = webglExperiments.js

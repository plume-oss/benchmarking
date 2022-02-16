Global / excludeLintKeys += lintUnused

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JavaServerAppPackaging)

name := "Plume Benchmarking"
version := "0.1"
scalaVersion := "2.13.7"
maintainer := "dbe@sun.ac.za"

idePackagePrefix := Some("com.github.plume.oss")
run := Defaults.runTask(fullClasspath in Runtime, mainClass in run in Compile, runner in run).evaluated

val plume_version = "1.0.12"
val moulting_yaml_version = "0.4.2"
val log4j_version = "2.17.1"
val javaMailVersion = "1.6.2"

libraryDependencies ++= Seq(
  "com.github.plume-oss" % "plume" % plume_version,
  "org.apache.logging.log4j" % "log4j-core" % log4j_version,
  "org.apache.logging.log4j" % "log4j-api" % log4j_version,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j_version,
  "net.jcazevedo" % "moultingyaml_2.13" % moulting_yaml_version,
  "com.sun.mail" % "javax.mail" % javaMailVersion
)

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  Resolver.jcenterRepo,
  "jitpack" at "https://jitpack.io"
)

mainClass in assembly := Some("com.github.plume.oss.Main")

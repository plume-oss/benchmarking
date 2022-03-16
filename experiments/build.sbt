Global / excludeLintKeys += lintUnused

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JavaServerAppPackaging)

name := "Plume Benchmarking"
version := "0.1"
scalaVersion := "2.13.7"
maintainer := "dbe@sun.ac.za"

idePackagePrefix := Some("com.github.plume.oss")
run := Defaults
  .runTask(
    Runtime / fullClasspath,
    Compile / run / mainClass,
    run / runner
  )
  .evaluated

val plume_version = "1.1.1"
val moulting_yaml_version = "0.4.2"
val logback_version = "1.2.11"
val javaMailVersion = "1.6.2"

libraryDependencies ++= Seq(
  "com.github.plume-oss" % "plume" % plume_version,
  "org.apache.logging.log4j" % "log4j-core" % "2.17.2",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2",
  "net.jcazevedo" % "moultingyaml_2.13" % moulting_yaml_version,
  "com.sun.mail" % "javax.mail" % javaMailVersion
)

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  Resolver.jcenterRepo,
  "jitpack" at "https://jitpack.io"
)

assembly / mainClass := Some("com.github.plume.oss.Main")

Global / excludeLintKeys += lintUnused

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JavaServerAppPackaging)

val plumeVersion = "1.1.7"
val moultingYamlVersion = "0.4.2"
val log4jVersion = "2.17.2"
val javaMailVersion = "1.6.2"

name := "Plume Benchmarking"
version := plumeVersion
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

libraryDependencies ++= Seq(
  "com.github.plume-oss" %% "plume" % plumeVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
  "net.jcazevedo" %% "moultingyaml" % moultingYamlVersion,
  "com.sun.mail" % "javax.mail" % javaMailVersion
)

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  Resolver.jcenterRepo,
  "jitpack" at "https://jitpack.io"
)

assembly / mainClass := Some("com.github.plume.oss.Main")

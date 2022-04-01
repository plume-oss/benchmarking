Global / excludeLintKeys += lintUnused

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JavaServerAppPackaging)

val plumeVersion = "1.2.1"
val moultingYamlVersion = "0.4.2"
val log4jVersion = "2.17.2"
val apacheCompressVersion = "1.21"
val zstdCompressVersion = "1.5.2-2"
val xzVersion = "1.9"
val lz4Version = "1.8.0"
val javaMailVersion = "1.6.2"

name := "Plume Benchmarking"
version := plumeVersion
scalaVersion := "2.13.8"
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
  "org.lz4" % "lz4-java" % lz4Version,
  "org.apache.commons" % "commons-compress" % apacheCompressVersion exclude ("org.lz4", "lz4-java"),
  "com.github.luben" % "zstd-jni" % zstdCompressVersion,
  "org.tukaani" % "xz" % xzVersion,
  "net.jcazevedo" %% "moultingyaml" % moultingYamlVersion,
  "com.sun.mail" % "javax.mail" % javaMailVersion
)

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  Resolver.jcenterRepo,
  "jitpack" at "https://jitpack.io",
  "Gradle Tooling" at "https://repo.gradle.org/gradle/libs-releases-local/"
)

assembly / mainClass := Some("com.github.plume.oss.Main")

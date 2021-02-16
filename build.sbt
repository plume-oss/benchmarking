name := "Plume Benchmarking"

version := "0.1"

scalaVersion := "2.13.4"

idePackagePrefix := Some("io.github.plume.oss")

val plume_version = "0.1.8"
val snakeyaml_version = "1.27"

libraryDependencies ++= Seq(
  "io.github.plume-oss"  % "plume" % plume_version exclude("io.github.plume-oss", "cpgconv"),
  "org.yaml" % "snakeyaml" % snakeyaml_version,
)

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  Resolver.jcenterRepo,
  "jitpack" at "https://jitpack.io",
)
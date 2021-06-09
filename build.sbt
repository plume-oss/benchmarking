enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JavaServerAppPackaging)

name := "Plume Benchmarking"
version := "0.1"
scalaVersion := "2.13.4"
maintainer := "dbe@sun.ac.za"

idePackagePrefix := Some("io.github.plume.oss")
run := Defaults.runTask(fullClasspath in Runtime, mainClass in run in Compile, runner in run).evaluated

val plume_version = "0.5.11"
val snakeyaml_version = "1.27"
val log4j_version = "2.11.2"
val circle_version = "0.14.0-M4"

libraryDependencies ++= Seq(
  "io.github.plume-oss" % "plume" % plume_version exclude ("io.github.plume-oss", "cpgconv"),
  "org.apache.logging.log4j" % "log4j-core" % log4j_version,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j_version,
  "org.yaml" % "snakeyaml" % snakeyaml_version
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circle_version)

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  Resolver.jcenterRepo,
  "jitpack" at "https://jitpack.io"
)

javaOptions in Universal += Seq(
  "-Djava.rmi.server.hostname=127.0.0.1",
  "-Dcom.sun.management.jmxremote.port=9090", // port of the rmi registery
  "-Dcom.sun.management.jmxremote.rmi.port=9090", // port of the rmi server
  "-Dcom.sun.management.jmxremote.ssl=false", // To disable SSL
  "-Dcom.sun.management.jmxremote.local.only=false", // when true, it indicates that the local JMX RMI connector will only accept connection requests from local interfaces
  "-Dcom.sun.management.jmxremote.authenticate=false", // Password authentication for remote monitoring is disabled
).mkString(" ")


mainClass in assembly := Some("io.github.plume.oss.Main")

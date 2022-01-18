enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(JavaServerAppPackaging)

name := "Plume Benchmarking"
version := "0.1"
scalaVersion := "2.13.4"
maintainer := "dbe@sun.ac.za"

idePackagePrefix := Some("com.github.plume.oss")
run := Defaults.runTask(fullClasspath in Runtime, mainClass in run in Compile, runner in run).evaluated

val plume_version = "1.0.2"
val moulting_yaml_version = "0.4.2"
val log4j_version = "2.17.0"

libraryDependencies ++= Seq(
  "com.github.plume-oss" % "plume" % plume_version,
  "org.apache.logging.log4j" % "log4j-core" % log4j_version,
  "net.jcazevedo" % "moultingyaml_2.13" % moulting_yaml_version,
)

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


mainClass in assembly := Some("com.github.plume.oss.Main")

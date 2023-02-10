name := "tilli-backend-server-2"

version := "1.0"

scalaVersion := "2.12.10"

lazy val root = (project in file("."))
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)

jvmOptions in MultiJvm := Seq("-Xmx256M")

enablePlugins(JavaServerAppPackaging, DockerPlugin)

val akkaVersion = "2.6.3"
val akkaHttpVersion = "10.1.11"
val akkaManagementVersion = "1.0.5"
val logbackVersion = "1.2.3"

libraryDependencies ++=Seq(
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "ch.megard" %% "akka-http-cors" % "0.4.1",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-jackson" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "org.json4s" %% "json4s-native" % "3.6.7",
  "org.json4s" %% "json4s-jackson" % "3.6.7",

  "commons-io" % "commons-io" % "2.6",
  "commons-lang" % "commons-lang" % "2.6",
  "org.apache.commons" % "commons-email" % "1.5",


  "org.projectlombok" % "lombok" % "1.16.16",
  "com.danielasfregola" %% "random-data-generator" % "2.8",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "io.lettuce" % "lettuce-core" % "6.0.0.M1",

  "org.rocksdb" % "rocksdbjni" % "6.11.4",
  "joda-time" % "joda-time" % "2.10.14",
  "commons-codec" % "commons-codec" % "1.11",
  "org.apache.poi" % "poi-ooxml" % "4.1.0",
)

mainClass in assembly := Some("com.teqbahn.bootstrap.StarterMain")

dockerBaseImage := "openjdk:8"

// For Cats
scalacOptions += "-Ypartial-unification"

assemblyMergeStrategy in assembly := {
  case x if x.contains("META-INF/io.netty.versions.properties") => MergeStrategy.discard
  case x if x.contains("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// ALPN agent, only required on JVM 8
enablePlugins(JavaAgent)
javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test"




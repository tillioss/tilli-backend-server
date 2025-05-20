name := "tilli-backend-server-2"

version := "1.0"

val scalaTestVersion = "3.2.9"

scalaVersion := "2.12.10"

lazy val root = (project in file("."))
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)

jvmOptions in MultiJvm := Seq("-Xmx256M")

enablePlugins(JavaServerAppPackaging, DockerPlugin)

val pekkoVersion = "1.0.2"
val pekkoHttpVersion = "1.0.1"
val logbackVersion = "1.2.3"

libraryDependencies ++=Seq(
  "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-http-core" % pekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-xml" % pekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.apache.pekko" %% "pekko-http-cors" % pekkoHttpVersion,
  "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
  "org.apache.pekko" %% "pekko-http-jackson" % pekkoHttpVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  "org.json4s" %% "json4s-native" % "3.6.7",
  "org.json4s" %% "json4s-jackson" % "3.6.7",
  "org.apache.pekko" %% "pekko-testkit" % pekkoVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.mockito" %% "mockito-scala" % "1.17.12" % Test,
  "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion % Test,
  "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
  "redis.clients" % "jedis" % "4.3.1" % Test, 
  "redis.clients" % "jedis" % "4.3.1",
  "commons-io" % "commons-io" % "2.6",
  "commons-lang" % "commons-lang" % "2.6",
  "org.apache.commons" % "commons-email" % "1.6.0",
  "org.projectlombok" % "lombok" % "1.16.16",
  "com.danielasfregola" %% "random-data-generator" % "2.8",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "io.lettuce" % "lettuce-core" % "6.0.0.M1",
  "org.rocksdb" % "rocksdbjni" % "6.11.4",
  "joda-time" % "joda-time" % "2.10.14",
  "commons-codec" % "commons-codec" % "1.11",
  "org.apache.poi" % "poi-ooxml" % "4.1.0",
  "org.mockito" % "mockito-core" % "5.8.0" % Test,
  "org.scalatestplus" %% "mockito-4-11" % "3.2.18.0" % Test,
  "com.icegreen" % "greenmail" % "2.0.1" % Test
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

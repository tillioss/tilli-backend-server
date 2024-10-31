name := "tilli-backend-server-2"

version := "1.0"

val scalaTestVersion = "3.2.9"

scalaVersion := "2.12.10"

lazy val root = (project in file("."))
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)

jvmOptions in MultiJvm := Seq("-Xmx256M")

enablePlugins(JavaServerAppPackaging, DockerPlugin)

val logbackVersion = "1.2.3"

libraryDependencies ++=Seq(
  "dev.zio" %% "zio" % "2.0.15",
  "dev.zio" %% "zio-streams" % "2.0.15",
  "dev.zio" %% "zio-actors" % "2.0.0",
  "io.lettuce" % "lettuce-core" % "6.2.4.RELEASE",
  "dev.zio" %% "zio-http" % "3.0.0",
  "com.typesafe" % "config" % "1.4.2"
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
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

name := "overland"

version := "0.1-SNAPSHOT"

mainClass in (Compile, run) := Some("ru.org.codingteam.overland.Application")

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.10" % "2.1.0"

libraryDependencies += "org.igniterealtime.smack" % "smack" % "3.2.1"

libraryDependencies += "org.igniterealtime.smack" % "smackx" % "3.2.1"

libraryDependencies += "org.mashupbots.socko" %% "socko-webserver" % "0.2.4"

libraryDependencies += "com.google.code.gson" % "gson" % "2.2.2"

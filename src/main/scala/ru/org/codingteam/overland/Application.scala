package ru.org.codingteam.overland

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import core.{WebSocketMessage, Core}
import org.mashupbots.socko.routes.{Path, Routes, WebSocketFrame, WebSocketHandshake}
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}
import scala.concurrent.duration._
import scala.language.postfixOps

object Application extends App {
  implicit val timeout = Timeout(60 seconds)

  val system = ActorSystem("CodingteamSystem")
  val core = system.actorOf(Props[Core], name = "core")

  val routes = Routes({
    case WebSocketHandshake(handshake) => handshake match {
      case Path("/websocket/") => {
        handshake.authorize()
      }
    }

    case WebSocketFrame(frame) => {
      core ! WebSocketMessage(frame)
    }
  })

  val webServer = new WebServer(WebServerConfig(port = 9000), routes, system)
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run { webServer.stop() }
  })
  webServer.start()
}

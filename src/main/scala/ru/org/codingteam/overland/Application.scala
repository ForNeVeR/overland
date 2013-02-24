package ru.org.codingteam.overland

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import core.{WebSocketMessage, Core}
import org.mashupbots.socko.routes.{Path, Routes, WebSocketFrame, WebSocketHandshake}
import akka.util.Timeout
import scala.concurrent.duration._
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}

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

  val webServer = new WebServer(WebServerConfig(), routes, system)
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run { webServer.stop() }
  })
  webServer.start()
}

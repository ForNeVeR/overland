package ru.org.codingteam.overland.websocket

import akka.actor.{Actor, ActorLogging}
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jivesoftware.smack.XMPPConnection
import com.google.gson.Gson

case class ConnectInfo(login: String, password: String)

object WebSocketProcessor {
  lazy val gson = new Gson()
}

class WebSocketProcessor extends Actor with ActorLogging {
  import WebSocketProcessor._

  var channel: Channel = null
  var connected: Boolean = false
  var connection: XMPPConnection = null

  override def receive = {
    case event: WebSocketFrameEvent if !connected =>
      val info = gson.fromJson(event.readText, classOf[ConnectInfo])
      channel = event.channel
      val server = "jabber.ru"

      connection = new XMPPConnection(server)
      connection.connect()
      connection.login(info.login, info.password)
      log.info("Login succeed")
      send("Login succeed")
      connected = true
  }

  def send(text: String) {
    channel.write(new TextWebSocketFrame(text))
  }
}

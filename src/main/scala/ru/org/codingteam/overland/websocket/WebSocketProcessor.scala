package ru.org.codingteam.overland.websocket

import akka.actor.{Actor, ActorLogging}
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jivesoftware.smack.XMPPConnection

class WebSocketProcessor extends Actor with ActorLogging {

  var channel: Channel = null
  var connected: Boolean = false
  var connection: XMPPConnection = null

  override def receive = {
    case event: WebSocketFrameEvent =>
      val text = event.readText
      if (isConnectInfo(text) && !connected) {
        channel = event.channel
        val server = "jabber.ru"
        val login = getLogin(text)
        val password = getPassword(text)

        connection = new XMPPConnection(server)
        connection.connect()
        connection.login(login, password)
        log.info("Login succeed")
        send("Login succeed")
        connected = true
      }
  }

  def isConnectInfo(text: String) = {
    text.startsWith("connect:")
  }

  def getLogin(text: String) = {
    text.split(':')(1)
  }

  def getPassword(text: String) = {
    text.split(':')(2)
  }

  def send(text: String) {
    channel.write(new TextWebSocketFrame(text))
  }
}

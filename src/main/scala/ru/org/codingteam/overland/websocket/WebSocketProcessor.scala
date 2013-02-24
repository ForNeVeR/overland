package ru.org.codingteam.overland.websocket

import akka.actor.{Actor, ActorLogging}
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jivesoftware.smack.{Chat, MessageListener, XMPPConnection}
import com.google.gson.Gson
import java.lang.Throwable
import org.jivesoftware.smack.packet.Message

case class ConnectInfo(login: String, password: String)
case class MessageInfo(to: String, text: String)

case class ChatMessage(from: String, text: String)

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

      try {
        connection = new XMPPConnection(server)
        connection.connect()
        connection.login(info.login, info.password)
        log.info("Login succeed")
        send("Login succeed")
        connected = true
      } catch {
        case exception: Throwable =>
          log.error(exception, "XMPP connection error")
          connected = false
          connection.disconnect()
          send(s"Connection error: $exception")
      }
    case event: WebSocketFrameEvent if connected =>
      val info = gson.fromJson(event.readText, classOf[MessageInfo])
      val chat = getChat(info.to)
      chat.sendMessage(info.text)
    case ChatMessage(from, text) =>
      send(s"$from: $text")
  }

  def send(text: String) {
    channel.write(new TextWebSocketFrame(text))
  }

  def getChat(jid: String) = {
    // TODO: remember created chats
    connection.getChatManager.createChat(jid, new MessageListener {
      def processMessage(chat: Chat, message: Message) {
        self ! ChatMessage(chat.getParticipant, message.getBody)
      }
    })
  }
}

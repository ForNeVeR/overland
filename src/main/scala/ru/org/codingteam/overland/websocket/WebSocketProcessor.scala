package ru.org.codingteam.overland.websocket

import akka.actor.{Actor, ActorLogging}
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jivesoftware.smack.{Chat, ConnectionConfiguration, ConnectionListener, MessageListener, XMPPConnection}
import com.google.gson.Gson
import java.lang.Throwable
import org.jivesoftware.smack.packet.Message

case class ConnectInfo(server: String, login: String, password: String)
case class MessageInfo(to: String, text: String)

case class ChatMessage(from: String, text: String)
case class CriticalError(message: String, error: Throwable)

object WebSocketProcessor {
  lazy val gson = new Gson()
}

class WebSocketProcessor extends Actor with ActorLogging {
  import WebSocketProcessor._

  var channel: Channel = null
  var connected: Boolean = false
  var connection: XMPPConnection = null
  var chats = Map[String, Chat]()

  override def receive = {
    case event: WebSocketFrameEvent if !connected =>
      val info = gson.fromJson(event.readText, classOf[ConnectInfo])
      channel = event.channel

      try {
        connect(info.server, info.login, info.password)
        send("Login succeed")
      } catch {
        case error: Throwable =>
          self ! CriticalError("Connection error", error)
      }

    case event: WebSocketFrameEvent if connected =>
      val info = gson.fromJson(event.readText, classOf[MessageInfo])
      val chat = getChat(info.to)
      chat.sendMessage(info.text)

    case ChatMessage(from, text) =>
      send(s"$from: $text")

    case CriticalError(message, error) =>
      publishError(message, error)
      context.stop(self)
  }

  override def postStop() {
    log.info("Stopping processor")
    if (connection != null) {
      log.info("Closing connection")
      try {
        connection.disconnect()
      } catch {
        case error: Throwable =>
          log.error(error, "Connection closing failed")
      }
    }
  }

  def connectionListener = new ConnectionListener {
    def reconnectionFailed(e: Exception) {
      self ! CriticalError("Reconnection failed", e)
    }

    def reconnectionSuccessful() {}

    def connectionClosedOnError(e: Exception) {
      self ! CriticalError("Connection closed on error", e)
    }

    def connectionClosed() {
      self ! CriticalError("Connection closed", null)
    }

    def reconnectingIn(seconds: Int) {}
  }

  def connect(server: String, login: String, password: String) {
    val port = 5222 // default XMPP port
    val configuration = new ConnectionConfiguration(server, port)
    configuration.setReconnectionAllowed(false)

    connection = new XMPPConnection(configuration)
    connection.connect()
    connection.addConnectionListener(connectionListener)
    connection.login(login, password)
    connected = true
  }

  def send(text: String) {
    log.info(s"Sending: $text")
    if (channel != null && channel.isWritable) {
      channel.write(new TextWebSocketFrame(text))
    } else {
      log.info(s"Detected that client offline. Terminating...")
      context.stop(self)
    }
  }

  def publishError(message: String, error: Throwable) {
    log.error(error, message)
    send(s"$message: $error")
  }

  def getChat(jid: String) = {
    val maybeChat = chats.get(jid)
    maybeChat match {
      case Some(chat) => chat
      case None => {
        val chat = createChat(jid)
        chats = chats.updated(jid, chat)
        chat
      }
    }
  }

  def createChat(jid: String) = {
    connection.getChatManager.createChat(jid, new MessageListener {
      def processMessage(chat: Chat, message: Message) {
        self ! ChatMessage(chat.getParticipant, message.getBody)
      }
    })
  }
}

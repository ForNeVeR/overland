package ru.org.codingteam.overland.websocket

import akka.actor.{Actor, ActorLogging}
import com.google.gson.Gson
import java.lang.Throwable
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jivesoftware.smack.{Chat, ConnectionConfiguration, XMPPConnection}
import org.jivesoftware.smackx.muc.MultiUserChat
import org.mashupbots.socko.events.WebSocketFrameEvent
import ru.org.codingteam.overland.xmpp.{XMPPConnectionListener, XMPPMessageListener, XMPPChatManagerListener}

object WebSocketProcessor {
  lazy val gson = new Gson()
}

class WebSocketProcessor extends Actor with ActorLogging {
  import WebSocketProcessor._

  var channel: Channel = null
  var connected: Boolean = false
  var connection: XMPPConnection = null

  var chats = Map[String, Chat]()
  var rooms = Map[String, MultiUserChat]()

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

  def connectionListener = new XMPPConnectionListener(self)

  def connect(server: String, login: String, password: String) {
    val port = 5222 // default XMPP port
    val configuration = new ConnectionConfiguration(server, port)
    configuration.setReconnectionAllowed(false)

    connection = new XMPPConnection(configuration)
    connection.connect()
    connection.addConnectionListener(connectionListener)
    connection.getChatManager.addChatListener(new XMPPChatManagerListener(self))
    connection.login(login, password, "overland")
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

  def getRoom(jid: String) = {
    val maybeRoom = rooms.get(jid)
    maybeRoom match {
      case Some(room) => room
      case None => {
        val room = createRoom(jid)
        rooms = rooms.updated(jid, room)
        room
      }
    }
  }

  def createChat(jid: String) = {
    connection.getChatManager.createChat(jid, new XMPPMessageListener(self))
  }

  def createRoom(jid: String) = {
    val muc = new MultiUserChat(connection, jid)
    // TODO: Register participant processor.
    // TODO: Register message processor.
    muc
  }
}

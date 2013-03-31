package ru.org.codingteam.overland.xmpp

import akka.actor.ActorRef
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.{Chat, MessageListener}
import ru.org.codingteam.overland.websocket.ChatMessage

class XMPPMessageListener(val processor: ActorRef) extends MessageListener {
  override def processMessage(chat: Chat, message: Message) {
    processor ! ChatMessage(chat.getParticipant, message.getBody)
  }
}

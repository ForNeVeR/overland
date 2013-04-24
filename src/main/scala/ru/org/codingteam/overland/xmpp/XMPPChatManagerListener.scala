package ru.org.codingteam.overland.xmpp

import akka.actor.ActorRef
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.{Chat, MessageListener, ChatManagerListener}
import ru.org.codingteam.overland.websocket.ChatMessage

class XMPPChatManagerListener(val processor: ActorRef) extends ChatManagerListener {
  override def chatCreated(chat: Chat, createdLocally: Boolean) {
    if (!createdLocally)
        chat.addMessageListener(new XMPPMessageListener(processor));
  }
}

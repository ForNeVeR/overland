package ru.org.codingteam.overland.xmpp

import akka.actor.ActorRef
import org.jivesoftware.smack.ConnectionListener
import ru.org.codingteam.overland.websocket.CriticalError


class XMPPConnectionListener(val processor: ActorRef) extends ConnectionListener {
  override def reconnectionFailed(e: Exception) {
    processor ! CriticalError("Reconnection failed", e)
  }

  override def reconnectionSuccessful() {}

  override def connectionClosedOnError(e: Exception) {
    processor ! CriticalError("Connection closed on error", e)
  }

  override def connectionClosed() {
    processor ! CriticalError("Connection closed", null)
  }

  override def reconnectingIn(seconds: Int) {}
}
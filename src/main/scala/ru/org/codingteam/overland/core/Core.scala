package ru.org.codingteam.overland.core

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import org.mashupbots.socko.handlers.WebSocketBroadcaster
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.jboss.netty.channel.Channel
import ru.org.codingteam.overland.websocket.WebSocketProcessor

case class WebSocketMessage(event: WebSocketFrameEvent)

class Core extends Actor with ActorLogging {
  var processors = Map[Channel, ActorRef]()

  def receive = {
    case WebSocketMessage(event) =>
      val channel = event.channel
      val someProcessor = processors.get(channel)
      val processor = someProcessor match {
        case Some(processor) => processor
        case None => {
          log.info(s"Creating processor for $channel")
          val processor = context.system.actorOf(Props[WebSocketProcessor])
          processors = processors.updated(channel, processor)
          processor
        }
      }

      processor ! event
  }
}

package ru.org.codingteam.overland.websocket

case class ConnectInfo(server: String, login: String, password: String)
case class MessageInfo(to: String, text: String)

case class ChatMessage(from: String, text: String)
case class CriticalError(message: String, error: Throwable)
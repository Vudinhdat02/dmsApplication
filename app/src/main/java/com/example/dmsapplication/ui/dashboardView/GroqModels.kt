package com.example.dmsapplication.ui.dashboardView

data class GroqRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class GroqResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
package edu.gatech.cog.names.models

data class Message(
    val text: String,
    var displayTime: Long = 0,
    var timeToAck: Long = 0
)
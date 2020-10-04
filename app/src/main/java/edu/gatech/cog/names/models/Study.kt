package edu.gatech.cog.names.models

data class Study(
    val sanitizedScript: List<Message>,
    val acknowledgedMessages: List<Message>,
)
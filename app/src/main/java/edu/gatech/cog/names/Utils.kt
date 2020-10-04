package edu.gatech.cog.names

import android.content.res.Resources
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import edu.gatech.cog.names.models.Message

fun loadScript(
    resources: Resources,
    participantName: String,
): List<Message> {
    val toReturn = mutableListOf<Message>()

    val listScriptMessage =
        Types.newParameterizedType(List::class.java, String::class.java)
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val adapter = moshi.adapter<List<String>>(listScriptMessage)

    val scriptTemplate = adapter.fromJson(
        resources.openRawResource(R.raw.namescript).bufferedReader().use { it.readText() })!!
    var names = adapter.fromJson(
        resources.openRawResource(R.raw.names)
            .bufferedReader().use { it.readText() })!!
        .filter { name -> name != participantName }
        .shuffled()

    var nameIndex = 0

    scriptTemplate.forEach { name ->
        val scriptMessageText = if (name == "PARTICIPANT") {
            participantName
        } else {
            if (nameIndex == names.size) {
                nameIndex = 0
                names = names.shuffled()
            }
            names[nameIndex++]
        }
        toReturn.add(Message(scriptMessageText))
    }

    return toReturn
}

fun waitDuration(message: String) =
    (8 + (message.length % 5)) * 1000L // 8 - 12 seconds wait

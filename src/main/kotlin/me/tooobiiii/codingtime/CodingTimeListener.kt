package me.tooobiiii.codingtime

import com.intellij.util.messages.Topic

/**
 * Fired whenever recorded coding time changes — either because a session was flushed to storage
 * or because the currently running session ticked forward.
 */
fun interface CodingTimeListener {

    fun codingTimeChanged()

    companion object {
        @Topic.AppLevel
        @JvmField
        val TOPIC: Topic<CodingTimeListener> =
            Topic(CodingTimeListener::class.java, Topic.BroadcastDirection.NONE)
    }
}

package com.ferbotz.momentic.presentation.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One-off events from a view model to its screen — navigate, open a URL, share. Unlike state, each
 * event is delivered once: buffered until the screen collects it, never replayed after a rotation.
 */
class EventChannel<T> {
    private val channel = Channel<T>(Channel.BUFFERED)

    val events: Flow<T> = channel.receiveAsFlow()

    fun send(event: T) {
        channel.trySend(event)
    }
}

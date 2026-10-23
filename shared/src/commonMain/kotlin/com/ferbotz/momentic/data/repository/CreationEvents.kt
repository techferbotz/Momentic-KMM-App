package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.model.Creation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Something happened to a creation that other open screens should reflect. */
sealed interface CreationChange {
    /** Created or updated (a draft save, a publish, a renewal). */
    data class Saved(val creation: Creation) : CreationChange

    data class Deleted(val creationId: String) : CreationChange

    /**
     * The set of creations this principal owns changed wholesale — a sign-in adopted the device's
     * drafts, or a sign-out switched back to the device. Lists should reload.
     */
    data object OwnerChanged : CreationChange
}

/**
 * App-wide bus of [CreationChange]s. The repositories publish after a server-confirmed change; list
 * and detail view models listen, so "My moments" never sits stale behind the editor or the paywall.
 */
class CreationEvents {
    private val _changes = MutableSharedFlow<CreationChange>(extraBufferCapacity = 16)
    val changes: SharedFlow<CreationChange> = _changes.asSharedFlow()

    fun publish(change: CreationChange) {
        _changes.tryEmit(change)
    }
}

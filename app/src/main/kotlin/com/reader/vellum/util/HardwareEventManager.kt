package com.reader.vellum.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class HardwareEvent {
    VOLUME_UP,
    VOLUME_DOWN
}

@Singleton
class HardwareEventManager @Inject constructor() {
    private val _events = MutableSharedFlow<HardwareEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HardwareEvent> = _events.asSharedFlow()

    fun emitEvent(event: HardwareEvent) {
        _events.tryEmit(event)
    }
}

package com.campusconnect.app.data

import com.campusconnect.app.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

data class ListState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = true,
    val offline: Boolean = false,
    val error: String? = null,
)

/**
 * Cache-first list: shows saved data instantly, then syncs from the network.
 * If the network fails but cached data exists, the UI shows an "offline" banner instead of an error.
 */
class CachedList<T>(
    private val scope: CoroutineScope,
    private val cache: Cache,
    private val key: String,
    private val serializer: KSerializer<List<T>>,
    private val fetch: suspend () -> List<T>,
) {
    private val _state = MutableStateFlow(ListState<T>())
    val state: StateFlow<ListState<T>> = _state.asStateFlow()
    private var job: Job? = null

    init { refresh(first = true) }

    fun refresh(first: Boolean = false) {
        job?.cancel()
        job = scope.launch {
            if (first) {
                withContext(Dispatchers.IO) { cache.read(key, serializer) }
                    ?.let { cached -> _state.value = ListState(cached, loading = true) }
            }
            _state.update { it.copy(loading = true, error = null) }
            try {
                val fresh = fetch()
                _state.value = ListState(fresh, loading = false)
                withContext(Dispatchers.IO) { cache.write(key, serializer, fresh) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        offline = it.items.isNotEmpty(),
                        error = if (it.items.isEmpty()) friendlyError(e) else null,
                    )
                }
            }
        }
    }
}

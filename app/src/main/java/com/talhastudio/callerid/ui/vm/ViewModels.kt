package com.talhastudio.callerid.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talhastudio.callerid.Graph
import com.talhastudio.callerid.core.*
import com.talhastudio.callerid.data.local.CallRecord
import com.talhastudio.callerid.data.remote.NOT_CONFIGURED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveCall(val to: String, val from: String, val session: CallSession)

class HomeViewModel : ViewModel() {
    private val repo = Graph.repo
    val digits = MutableStateFlow("")
    val callerId = MutableStateFlow<VerifiedNumber?>(null)
    val active = MutableStateFlow<ActiveCall?>(null)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val recent: StateFlow<List<CallRecord>> =
        repo.recent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private var pollJob: Job? = null

    fun press(d: String) { if (digits.value.length < 11) digits.value += d }
    fun backspace() { digits.value = digits.value.dropLast(1) }

    fun refresh() {
        if (!repo.configured) return
        viewModelScope.launch {
            repo.numbers().onSuccess { list -> callerId.value = list.firstOrNull { it.isDefault && it.isVerified } }
        }
    }

    fun call() {
        if (active.value != null) return
        viewModelScope.launch {
            if (!repo.configured) { messages.tryEmit(NOT_CONFIGURED); return@launch }
            val to = PakistaniNumber.normalize(digits.value)
            if (to == null) { messages.tryEmit("Enter a valid Pakistani mobile number."); return@launch }
            val mode = repo.mode().getOrNull()
            if (mode != null && !mode.providerConfigured) { messages.tryEmit(NOT_CONFIGURED); return@launch }
            val from = callerId.value
            if (from == null) { messages.tryEmit("Add and verify a caller ID first."); return@launch }
            repo.provider.makeCall(to, from.id)
                .onFailure { messages.tryEmit(it.message ?: "The call could not be started.") }
                .onSuccess { poll(to, from.e164, it) }
        }
    }

    private fun poll(to: String, from: String, first: CallSession) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var s = first
            var failures = 0
            val startedAt = System.currentTimeMillis()
            active.value = ActiveCall(to, from, s)
            while (!s.state.terminal) {
                delay(1000)
                val r = repo.provider.getCallStatus(s.callId)
                r.onSuccess { s = it; failures = 0; active.value = ActiveCall(to, from, it) }
                r.onFailure { failures++; if (failures >= 3) s = s.copy(state = CallState.FAILED) }
            }
            active.value = ActiveCall(to, from, s)
            repo.saveCall(CallRecord(toNumber = to, callerId = from, startedAt = startedAt,
                durationSec = s.durationSec, status = s.state.name, testMode = s.testMode))
        }
    }

    fun hangUp() {
        val id = active.value?.session?.callId ?: return
        viewModelScope.launch {
            repo.provider.endCall(id).onFailure { messages.tryEmit(it.message ?: "Couldn't end the call.") }
        }
    }

    fun dismiss() { if (active.value?.session?.state?.terminal == true) active.value = null }
}

class VerifiedNumbersViewModel : ViewModel() {
    private val repo = Graph.repo
    val numbers = MutableStateFlow<List<VerifiedNumber>>(emptyList())
    val busy = MutableStateFlow(false)
    val testMode = MutableStateFlow(false)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun refresh() {
        viewModelScope.launch {
            repo.numbers().onSuccess { numbers.value = it }.onFailure { messages.tryEmit(it.message ?: "Couldn't load numbers.") }
            repo.mode().onSuccess { testMode.value = it.mode == "mock" }
        }
    }

    fun add(raw: String, onAdded: (VerifiedNumber) -> Unit) {
        val e164 = PakistaniNumber.parseCallerId(raw)
        if (e164 == null) { messages.tryEmit("Enter a valid number, like 03001234567 or +442071838750."); return }
        viewModelScope.launch {
            busy.value = true
            repo.provider.requestCallerIdVerification(e164)
                .onSuccess { refresh(); onAdded(it) }
                .onFailure { messages.tryEmit(it.message ?: "Couldn't add the number.") }
            busy.value = false
        }
    }

    fun verify(id: String, code: String, onDone: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            repo.provider.verifyCallerId(id, code.trim())
                .onSuccess { refresh(); onDone() }
                .onFailure { messages.tryEmit(it.message ?: "Verification failed.") }
            busy.value = false
        }
    }

    fun makeDefault(id: String) = viewModelScope.launch {
        repo.setDefault(id).onSuccess { refresh() }.onFailure { messages.tryEmit(it.message ?: "Couldn't change the default.") }
    }

    fun remove(id: String) = viewModelScope.launch {
        repo.removeNumber(id).onSuccess { refresh() }.onFailure { messages.tryEmit(it.message ?: "Couldn't remove the number.") }
    }
}

class CallerNameViewModel : ViewModel() {
    private val repo = Graph.repo
    val current = MutableStateFlow(CallerName(null, "none"))
    val busy = MutableStateFlow(false)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun refresh() {
        viewModelScope.launch {
            repo.callerName().onSuccess { current.value = it }.onFailure { messages.tryEmit(it.message ?: "Couldn't load the caller name.") }
        }
    }

    fun submit(name: String) {
        if (!PakistaniNumber.isValidCallerName(name)) {
            messages.tryEmit("Use 2 to 30 letters, numbers or basic punctuation.")
            return
        }
        viewModelScope.launch {
            busy.value = true
            repo.provider.setApprovedCallerName(name.trim())
                .onSuccess { current.value = it }
                .onFailure { messages.tryEmit(it.message ?: "Couldn't submit the name.") }
            busy.value = false
        }
    }
}

class HistoryViewModel : ViewModel() {
    private val repo = Graph.repo
    val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<CallRecord>> = query
        .flatMapLatest { q -> repo.search(q.filter { it.isDigit() }.removePrefix("0")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() { viewModelScope.launch { repo.clearHistory() } }
}

class ProviderViewModel : ViewModel() {
    private val repo = Graph.repo
    val mode = MutableStateFlow<String?>(null)
    val balance = MutableStateFlow<String?>(null)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun refresh() {
        viewModelScope.launch {
            repo.mode().onSuccess { mode.value = it.mode }.onFailure { messages.tryEmit(it.message ?: "Couldn't reach the backend.") }
        }
    }

    fun loadBalance() {
        viewModelScope.launch {
            repo.provider.getAccountBalance()
                .onSuccess { balance.value = if (it.amount == null) "Not available" else "${it.amount} ${it.currency ?: ""}".trim() }
                .onFailure { messages.tryEmit(it.message ?: "Couldn't load the balance.") }
        }
    }
}

class LoginViewModel : ViewModel() {
    private val repo = Graph.repo
    val busy = MutableStateFlow(false)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun submit(email: String, password: String, create: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            repo.signIn(email, password, create)
                .onSuccess { onSuccess() }
                .onFailure { messages.tryEmit(it.message ?: "Couldn't sign in.") }
            busy.value = false
        }
    }
}

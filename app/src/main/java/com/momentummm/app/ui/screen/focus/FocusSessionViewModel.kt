package com.momentummm.app.ui.screen.focus

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.service.FocusSessionState
import com.momentummm.app.service.FocusTimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FocusSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val _sessionState = MutableStateFlow(FocusSessionState())
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

    private var boundService: FocusTimerService? = null
    private var serviceStateJob: Job? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? FocusTimerService.FocusTimerBinder ?: return
            boundService = binder.getService()
            isBound = true
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            boundService = null
            serviceStateJob?.cancel()
        }
    }

    init {
        bindService()
    }

    fun startSession(session: FocusSession) {
        val intent = Intent(appContext, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_SESSION_TYPE, session.id)
            putExtra(FocusTimerService.EXTRA_SESSION_NAME, session.name)
            putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, session.duration)
            putExtra(FocusTimerService.EXTRA_BREAK_MINUTES, session.breakDuration)
            putStringArrayListExtra(FocusTimerService.EXTRA_BLOCKED_APPS, ArrayList(session.blockedApps))
        }
        FocusTimerService.startForegroundService(appContext, intent)
        if (!isBound) {
            bindService()
        }
    }

    fun pauseSession() {
        val intent = Intent(appContext, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_PAUSE
        }
        appContext.startService(intent)
    }

    fun resumeSession() {
        val intent = Intent(appContext, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_RESUME
        }
        appContext.startService(intent)
    }

    fun stopSession() {
        val intent = Intent(appContext, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    private fun bindService() {
        val intent = Intent(appContext, FocusTimerService::class.java)
        appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun observeServiceState() {
        serviceStateJob?.cancel()
        val service = boundService ?: return
        serviceStateJob = viewModelScope.launch {
            service.sessionState.collect { state ->
                _sessionState.value = state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceStateJob?.cancel()
        if (isBound) {
            appContext.unbindService(connection)
        }
    }
}

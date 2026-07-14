package com.aichat.app.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.terminal.TerminalLogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val terminal: TerminalLogBuffer
) : ViewModel() {

    val logs = terminal.logs

    fun clear() {
        viewModelScope.launch { terminal.clear() }
    }
}

package com.zerodea.nfk.sample

import NfcCard
import NfcDetectorFactory
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val nfk = NfcDetectorFactory.getInstance()

    private val _readTag: MutableStateFlow<NfcCard?> = MutableStateFlow(viewModelScope, null)
    val readTag: StateFlow<NfcCard?> = _readTag

    fun startReading() {
        viewModelScope.launch {
            nfk.detect<NfcCard>(cardTypesToDetect = null, timeout = 5000L)?.let {
                println("read tag returned $it")
                _readTag.emit(it)
            } ?: println("read tag returned null")
        }
    }

    fun isEnabled(): Boolean {
        return nfk.isEnabled()
    }
}
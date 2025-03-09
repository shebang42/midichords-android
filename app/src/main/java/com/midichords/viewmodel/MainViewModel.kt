package com.midichords.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
  private val _connectionState = MutableLiveData<ConnectionState>()
  val connectionState: LiveData<ConnectionState> = _connectionState

  init {
    _connectionState.value = ConnectionState.DISCONNECTED
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
} 
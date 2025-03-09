package com.midichords.viewmodel

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.midichords.midi.ConnectionStateListener
import com.midichords.midi.MidiDeviceManagerImpl

class MainViewModel(application: Application) : AndroidViewModel(application), ConnectionStateListener {
  private val _connectionState = MutableLiveData<ConnectionState>()
  val connectionState: LiveData<ConnectionState> = _connectionState

  private val _connectionMessage = MutableLiveData<String>()
  val connectionMessage: LiveData<String> = _connectionMessage

  private val _availableDevices = MutableLiveData<List<UsbDevice>>()
  val availableDevices: LiveData<List<UsbDevice>> = _availableDevices

  private val midiDeviceManager = MidiDeviceManagerImpl(application)

  init {
    midiDeviceManager.registerListener(this)
    _connectionState.value = ConnectionState.DISCONNECTED
    refreshAvailableDevices()
  }

  override fun onConnectionStateChanged(state: ConnectionState, message: String?) {
    _connectionState.postValue(state)
    message?.let { _connectionMessage.postValue(it) }
  }

  fun refreshAvailableDevices() {
    _availableDevices.value = midiDeviceManager.getAvailableDevices()
  }

  fun connectToDevice(device: UsbDevice) {
    midiDeviceManager.requestPermission(device)
  }

  fun disconnect() {
    midiDeviceManager.disconnect()
  }

  override fun onCleared() {
    super.onCleared()
    midiDeviceManager.dispose()
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
} 
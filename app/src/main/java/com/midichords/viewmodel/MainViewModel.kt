package com.midichords.viewmodel

import android.app.Application
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.midichords.midi.ConnectionStateListener
import com.midichords.midi.MidiDeviceManagerImpl

class MainViewModel(application: Application) : AndroidViewModel(application), ConnectionStateListener {
  companion object {
    private const val TAG = "MainViewModel"
  }

  private val _connectionState = MutableLiveData<ConnectionState>()
  val connectionState: LiveData<ConnectionState> = _connectionState

  private val _connectionMessage = MutableLiveData<String>()
  val connectionMessage: LiveData<String> = _connectionMessage

  private val _availableDevices = MutableLiveData<List<UsbDevice>>()
  val availableDevices: LiveData<List<UsbDevice>> = _availableDevices

  private val midiDeviceManager = try {
    MidiDeviceManagerImpl(application).also {
      it.registerListener(this)
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize MIDI device manager", e)
    null
  }

  init {
    Log.d(TAG, "Initializing MainViewModel")
    _connectionState.value = ConnectionState.DISCONNECTED
    refreshAvailableDevices()
  }

  override fun onConnectionStateChanged(state: ConnectionState, message: String?) {
    Log.d(TAG, "Connection state changed to: $state, message: $message")
    _connectionState.postValue(state)
    message?.let { _connectionMessage.postValue(it) }
  }

  fun refreshAvailableDevices() {
    try {
      val devices = midiDeviceManager?.getAvailableDevices() ?: emptyList()
      Log.d(TAG, "Found ${devices.size} available devices")
      _availableDevices.value = devices
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing available devices", e)
      _connectionMessage.postValue("Error checking available devices: ${e.message}")
    }
  }

  fun connectToDevice(device: UsbDevice) {
    try {
      midiDeviceManager?.connectToDevice(device) ?: run {
        Log.e(TAG, "Cannot connect - MIDI manager not initialized")
        _connectionMessage.postValue("MIDI system not available")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to device", e)
      _connectionMessage.postValue("Error connecting to device: ${e.message}")
    }
  }

  fun disconnect() {
    try {
      midiDeviceManager?.disconnect()
    } catch (e: Exception) {
      Log.e(TAG, "Error disconnecting", e)
      _connectionMessage.postValue("Error disconnecting: ${e.message}")
    }
  }

  override fun onCleared() {
    super.onCleared()
    try {
      midiDeviceManager?.dispose()
    } catch (e: Exception) {
      Log.e(TAG, "Error disposing MIDI manager", e)
    }
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
} 
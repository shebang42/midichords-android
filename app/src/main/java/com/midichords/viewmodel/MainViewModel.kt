package com.midichords.viewmodel

import android.app.Application
import android.content.Context
import android.media.midi.MidiManager
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.midichords.midi.ConnectionState
import com.midichords.midi.MidiDeviceListener
import com.midichords.midi.MidiDeviceManager
import com.midichords.midi.MidiDeviceManagerImpl
import com.midichords.midi.MidiEvent
import com.midichords.midi.MidiEventListener

class MainViewModel(application: Application) : AndroidViewModel(application), MidiDeviceListener, MidiEventListener {
  companion object {
    private const val TAG = "MainViewModel"
  }

  private val _connectionState = MutableLiveData<ConnectionState>()
  val connectionState: LiveData<ConnectionState> = _connectionState

  private val _connectionMessage = MutableLiveData<String>()
  val connectionMessage: LiveData<String> = _connectionMessage

  private val _availableDevices = MutableLiveData<List<UsbDevice>>()
  val availableDevices: LiveData<List<UsbDevice>> = _availableDevices

  private val _lastMidiEvent = MutableLiveData<MidiEvent>()
  val lastMidiEvent: LiveData<MidiEvent> = _lastMidiEvent

  private val midiDeviceManager = try {
    val midiManager = application.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    MidiDeviceManagerImpl(application, midiManager, usbManager).also {
      it.registerListener(this)
      it.addMidiEventListener(this)
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize MIDI device manager", e)
    null
  }

  init {
    Log.d(TAG, "Initializing MainViewModel")
    _connectionState.value = ConnectionState.DISCONNECTED
    _connectionMessage.value = "Disconnected"
    refreshAvailableDevices()
  }

  override fun onConnectionStateChanged(state: ConnectionState, message: String) {
    Log.d(TAG, "Connection state changed: $state - $message")
    _connectionState.value = state
    _connectionMessage.value = message
  }

  fun refreshAvailableDevices() {
    try {
      midiDeviceManager?.refreshAvailableDevices()
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing devices", e)
      _connectionState.value = ConnectionState.ERROR
      _connectionMessage.value = "Error refreshing devices: ${e.message}"
    }
  }

  fun connectToDevice(device: UsbDevice) {
    try {
      midiDeviceManager?.connectToUsbDevice(device) ?: run {
        Log.e(TAG, "Cannot connect - MIDI manager not initialized")
        _connectionMessage.value = "MIDI system not available"
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to device", e)
      _connectionState.value = ConnectionState.ERROR
      _connectionMessage.value = "Error connecting to device: ${e.message}"
    }
  }

  fun disconnect() {
    try {
      midiDeviceManager?.disconnect()
    } catch (e: Exception) {
      Log.e(TAG, "Error disconnecting", e)
      _connectionState.value = ConnectionState.ERROR
      _connectionMessage.value = "Error disconnecting: ${e.message}"
    }
  }

  override fun onMidiEvent(event: MidiEvent) {
    Log.d(TAG, "MIDI event received: $event")
    _lastMidiEvent.value = event
  }

  override fun onCleared() {
    super.onCleared()
    midiDeviceManager?.unregisterListener(this)
    midiDeviceManager?.removeMidiEventListener(this)
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
} 
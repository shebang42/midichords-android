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
import com.midichords.model.ActiveNote
import com.midichords.model.BasicChordIdentifier
import com.midichords.model.Chord
import com.midichords.model.ChordIdentifier
import com.midichords.model.ChordListener
import com.midichords.model.NoteProcessor
import com.midichords.model.NoteProcessorImpl
import com.midichords.model.NoteStateListener

class MainViewModel(application: Application) : AndroidViewModel(application), MidiDeviceListener, NoteStateListener, ChordListener {
  companion object {
    private const val TAG = "MainViewModel"
  }

  private val _connectionState = MutableLiveData<ConnectionState>()
  val connectionState: LiveData<ConnectionState> = _connectionState

  private val _connectionMessage = MutableLiveData<String>()
  val connectionMessage: LiveData<String> = _connectionMessage

  private val _availableDevices = MutableLiveData<List<UsbDevice>>()
  val availableDevices: LiveData<List<UsbDevice>> = _availableDevices

  private val _activeNotes = MutableLiveData<List<ActiveNote>>()
  val activeNotes: LiveData<List<ActiveNote>> = _activeNotes

  private val _currentChord = MutableLiveData<Chord?>()
  val currentChord: LiveData<Chord?> = _currentChord

  private val _lastMidiEvent = MutableLiveData<MidiEvent>()
  val lastMidiEvent: LiveData<MidiEvent> = _lastMidiEvent
  
  private val usbManager: UsbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager

  private val noteProcessor: NoteProcessor = NoteProcessorImpl()
  private val chordIdentifier: ChordIdentifier = BasicChordIdentifier().apply {
    registerChordListener(this@MainViewModel)
  }

  private val midiDeviceManager = try {
    val midiManager = application.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    MidiDeviceManagerImpl(application, midiManager, usbManager).also {
      it.registerListener(this)
      it.addMidiEventListener(noteProcessor)
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize MIDI device manager", e)
    null
  }

  init {
    Log.d(TAG, "Initializing MainViewModel")
    _connectionState.value = ConnectionState.DISCONNECTED
    _connectionMessage.value = "Disconnected"
    _activeNotes.value = emptyList()
    _currentChord.value = null
    noteProcessor.registerNoteListener(this)
    
    // Connect the chord identifier to the note processor
    if (chordIdentifier is NoteStateListener) {
      noteProcessor.registerNoteListener(chordIdentifier as NoteStateListener)
    }
    
    refreshAvailableDevices()
  }

  override fun onConnectionStateChanged(state: ConnectionState, message: String) {
    Log.d(TAG, "Connection state changed: $state - $message")
    _connectionState.value = state
    _connectionMessage.value = message
  }

  fun refreshAvailableDevices() {
    try {
      // First, scan for USB devices directly
      scanForUsbDevices()
      
      // Then, let the MIDI device manager try to find MIDI devices
      midiDeviceManager?.refreshAvailableDevices()
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing devices", e)
      _connectionState.value = ConnectionState.ERROR
      _connectionMessage.value = "Error refreshing devices: ${e.message}"
    }
  }
  
  /**
   * Scan for USB devices directly using UsbManager
   */
  private fun scanForUsbDevices() {
    try {
      val deviceList = usbManager.deviceList
      Log.d(TAG, "Found ${deviceList.size} USB devices via UsbManager")
      
      // Update the available devices LiveData
      _availableDevices.value = deviceList.values.toList()
      
      // Log details about each device
      deviceList.forEach { (name, device) ->
        Log.d(TAG, "USB Device: $name")
        Log.d(TAG, "  Device ID: ${device.deviceId}")
        Log.d(TAG, "  Product ID: ${device.productId}")
        Log.d(TAG, "  Vendor ID: ${device.vendorId}")
        
        // Check if this device has a MIDI interface
        var hasMidiInterface = false
        for (i in 0 until device.interfaceCount) {
          val usbInterface = device.getInterface(i)
          if (usbInterface.interfaceClass == 1 && usbInterface.interfaceSubclass == 3) {
            hasMidiInterface = true
            Log.d(TAG, "  Interface $i is a MIDI interface")
          }
        }
        
        if (hasMidiInterface) {
          Log.d(TAG, "  This device has a MIDI interface")
        } else {
          Log.d(TAG, "  This device does not have a MIDI interface")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error scanning for USB devices", e)
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

  // NoteStateListener implementation
  override fun onNoteActivated(note: ActiveNote) {
    Log.d(TAG, "Note activated: ${note.getNoteName()}")
  }

  override fun onNoteDeactivated(note: ActiveNote) {
    Log.d(TAG, "Note deactivated: ${note.getNoteName()}")
  }

  override fun onActiveNotesChanged(activeNotes: List<ActiveNote>) {
    Log.d(TAG, "Active notes changed: ${activeNotes.size} notes")
    _activeNotes.postValue(activeNotes)
  }

  override fun onSustainPedalStateChanged(isOn: Boolean) {
    Log.d(TAG, "Sustain pedal state changed: $isOn")
  }

  // ChordListener implementation
  override fun onChordIdentified(chord: Chord, notes: List<ActiveNote>) {
    Log.d(TAG, "Chord identified: ${chord.getName()}")
    _currentChord.postValue(chord)
  }

  override fun onNoChordIdentified(notes: List<ActiveNote>) {
    Log.d(TAG, "No chord identified")
    _currentChord.postValue(null)
  }

  override fun onCleared() {
    super.onCleared()
    midiDeviceManager?.unregisterListener(this)
    midiDeviceManager?.removeMidiEventListener(noteProcessor)
    noteProcessor.unregisterNoteListener(this)
    
    if (chordIdentifier is NoteStateListener) {
      noteProcessor.unregisterNoteListener(chordIdentifier as NoteStateListener)
    }
    
    chordIdentifier.unregisterChordListener(this)
    
    // Clean up the MIDI device manager resources
    midiDeviceManager?.cleanup()
    
    Log.d(TAG, "MainViewModel cleared and resources cleaned up")
  }
}

enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
} 
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
import com.midichords.midi.MidiEventType
import com.midichords.model.ActiveNote
import com.midichords.model.BasicChordIdentifier
import com.midichords.model.Chord
import com.midichords.model.ChordIdentifier
import com.midichords.model.ChordListener
import com.midichords.model.NoteProcessor
import com.midichords.model.NoteProcessorImpl
import com.midichords.model.NoteStateListener

class MainViewModel(application: Application) : AndroidViewModel(application), MidiDeviceListener, NoteStateListener, ChordListener, MidiEventListener {
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
      // Get USB devices directly from UsbManager
      val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
      val allDevices = usbManager.deviceList.values.toList()
      
      // Filter out 0xBDA devices
      val filteredDevices = allDevices.filter { device -> device.vendorId != 0x0BDA }
      
      Log.d(TAG, "Found ${allDevices.size} USB devices via UsbManager, ${filteredDevices.size} after filtering out 0xBDA devices")
      
      // Log all devices for debugging
      allDevices.forEach { device ->
        val vendorId = "0x${device.vendorId.toString(16).uppercase()}"
        val productId = "0x${device.productId.toString(16).uppercase()}"
        val isBlocked = device.vendorId == 0x0BDA
        Log.d(TAG, "USB Device: ${device.deviceName}, VID: $vendorId, PID: $productId, Blocked: $isBlocked")
      }
      
      // Only update with non-0xBDA devices
      _availableDevices.value = filteredDevices
      
      if (filteredDevices.isEmpty() && allDevices.isNotEmpty()) {
        // We have devices but they're all 0xBDA
        _connectionState.value = ConnectionState.ERROR
        _connectionMessage.value = "Only found USB converter devices (0xBDA). Please connect a MIDI device directly."
      }
      
      // Also let the MIDI device manager refresh, but it will also filter out 0xBDA devices
      midiDeviceManager.refreshAvailableDevices()
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing devices", e)
      _connectionState.value = ConnectionState.ERROR
      _connectionMessage.value = "Error refreshing devices: ${e.message}"
    }
  }

  fun connectToDevice(device: UsbDevice) {
    // HARD BLOCK: Refuse to connect to 0xBDA devices
    if (device.vendorId == 0x0BDA) {
      Log.e(TAG, "BLOCKED: Refusing to connect to 0xBDA converter device: ${device.deviceName}")
      _connectionState.value = ConnectionState.ERROR
      _connectionMessage.value = "BLOCKED: Cannot connect to USB converter device (0xBDA). Please connect a MIDI device directly."
      return
    }
    
    try {
      Log.d(TAG, "Connecting to device: ${device.deviceName} (VID: 0x${device.vendorId.toString(16).uppercase()}, PID: 0x${device.productId.toString(16).uppercase()})")
      midiDeviceManager.connectToUsbDevice(device)
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
  
  /**
   * Send a test MIDI message to verify the MIDI event processing pipeline
   */
  fun sendTestMidiMessage() {
    Log.d(TAG, "Sending test MIDI message")
    
    // Create a test Note On event
    val testEvent = MidiEvent(
      type = MidiEventType.NOTE_ON,
      channel = 0,
      data1 = 60, // Middle C
      data2 = 100 // Velocity
    )
    
    // Process the event directly
    Log.d(TAG, "Processing test MIDI event: ${testEvent.type}, note: ${testEvent.data1}, velocity: ${testEvent.data2}")
    
    // Update the UI
    _lastMidiEvent.postValue(testEvent)
    
    // Send to the note processor
    noteProcessor.onMidiEvent(testEvent)
    
    // After 500ms, send a Note Off event
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      val noteOffEvent = MidiEvent(
        type = MidiEventType.NOTE_OFF,
        channel = 0,
        data1 = 60, // Middle C
        data2 = 0 // Velocity
      )
      
      Log.d(TAG, "Processing test MIDI Note Off event")
      
      // Update the UI
      _lastMidiEvent.postValue(noteOffEvent)
      
      // Send to the note processor
      noteProcessor.onMidiEvent(noteOffEvent)
    }, 500)
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
  
  // MidiEventListener implementation
  override fun onMidiEvent(event: MidiEvent) {
    // Only update UI for note events to avoid flooding the UI with other events
    if (event.type == MidiEventType.NOTE_ON || event.type == MidiEventType.NOTE_OFF) {
      Log.d(TAG, "MIDI event: ${event.type}, note: ${event.data1}, velocity: ${event.data2}, channel: ${event.channel}")
      _lastMidiEvent.postValue(event)
    }
  }
  
  /**
   * Get a human-readable note name from a MIDI note number
   */
  fun getNoteNameFromNumber(noteNumber: Int): String {
    val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octave = (noteNumber / 12) - 1
    val note = noteNumber % 12
    return "${noteNames[note]}$octave"
  }

  override fun onCleared() {
    super.onCleared()
    midiDeviceManager?.unregisterListener(this)
    midiDeviceManager?.removeMidiEventListener(noteProcessor)
    midiDeviceManager?.removeMidiEventListener(this)
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
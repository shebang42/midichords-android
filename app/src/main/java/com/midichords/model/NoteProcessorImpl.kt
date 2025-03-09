package com.midichords.model

import android.util.Log
import com.midichords.midi.MidiEvent
import com.midichords.midi.MidiEventType
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of the NoteProcessor interface that processes MIDI events
 * and tracks active notes.
 */
class NoteProcessorImpl : NoteProcessor {
  companion object {
    private const val TAG = "NoteProcessorImpl"
    private const val SUSTAIN_PEDAL_CONTROLLER = 64
    private const val SUSTAIN_THRESHOLD = 64
  }

  private val activeNotes = CopyOnWriteArrayList<ActiveNote>()
  private val listeners = CopyOnWriteArrayList<NoteStateListener>()
  private var isSustainOn = false

  override fun onMidiEvent(event: MidiEvent) {
    when (event.type) {
      MidiEventType.NOTE_ON -> processNoteOn(event)
      MidiEventType.NOTE_OFF -> processNoteOff(event)
      MidiEventType.CONTROL_CHANGE -> processControlChange(event)
      else -> {
        // Ignore other event types
      }
    }
  }

  private fun processNoteOn(event: MidiEvent) {
    // Note On with velocity 0 is treated as Note Off
    if (event.data2 == 0) {
      processNoteOff(event)
      return
    }

    val noteNumber = event.data1
    val velocity = event.data2
    val channel = event.channel
    val timestamp = System.currentTimeMillis()

    // Check if the note is already active
    val existingNote = findActiveNote(noteNumber, channel)
    if (existingNote != null) {
      // Update the existing note (re-trigger)
      activeNotes.remove(existingNote)
      val updatedNote = ActiveNote(noteNumber, velocity, channel, timestamp)
      activeNotes.add(updatedNote)
      notifyNoteActivated(updatedNote)
    } else {
      // Add a new note
      val newNote = ActiveNote(noteNumber, velocity, channel, timestamp)
      activeNotes.add(newNote)
      notifyNoteActivated(newNote)
    }

    notifyActiveNotesChanged()
    Log.d(TAG, "Note On: $noteNumber, velocity: $velocity, active notes: ${activeNotes.size}")
  }

  private fun processNoteOff(event: MidiEvent) {
    val noteNumber = event.data1
    val channel = event.channel

    // Find the active note
    val note = findActiveNote(noteNumber, channel) ?: return

    if (isSustainOn) {
      // Mark the note as sustained but don't remove it
      note.isSustained = true
      Log.d(TAG, "Note Off (sustained): $noteNumber")
    } else {
      // Remove the note from active notes
      activeNotes.remove(note)
      notifyNoteDeactivated(note)
      notifyActiveNotesChanged()
      Log.d(TAG, "Note Off: $noteNumber, active notes: ${activeNotes.size}")
    }
  }

  private fun processControlChange(event: MidiEvent) {
    if (event.data1 == SUSTAIN_PEDAL_CONTROLLER) {
      val isOn = event.data2 >= SUSTAIN_THRESHOLD
      setSustainPedal(isOn)
    }
  }

  override fun getActiveNotes(): List<ActiveNote> {
    return activeNotes.toList()
  }

  override fun isNoteActive(noteNumber: Int, channel: Int): Boolean {
    return findActiveNote(noteNumber, channel) != null
  }

  override fun getActiveNoteCount(): Int {
    return activeNotes.size
  }

  override fun setSustainPedal(isOn: Boolean) {
    if (isSustainOn == isOn) {
      return // No change
    }

    isSustainOn = isOn
    Log.d(TAG, "Sustain pedal: ${if (isOn) "ON" else "OFF"}")

    // If sustain is turned off, release all sustained notes
    if (!isOn) {
      val sustainedNotes = activeNotes.filter { it.isSustained }.toList()
      for (note in sustainedNotes) {
        activeNotes.remove(note)
        notifyNoteDeactivated(note)
      }
      if (sustainedNotes.isNotEmpty()) {
        notifyActiveNotesChanged()
      }
    }

    notifySustainPedalStateChanged(isOn)
  }

  override fun isSustainPedalOn(): Boolean {
    return isSustainOn
  }

  override fun registerNoteListener(listener: NoteStateListener) {
    listeners.add(listener)
  }

  override fun unregisterNoteListener(listener: NoteStateListener) {
    listeners.remove(listener)
  }

  private fun findActiveNote(noteNumber: Int, channel: Int): ActiveNote? {
    return if (channel == -1) {
      // Find by note number only (any channel)
      activeNotes.find { it.noteNumber == noteNumber }
    } else {
      // Find by note number and channel
      activeNotes.find { it.noteNumber == noteNumber && it.channel == channel }
    }
  }

  private fun notifyNoteActivated(note: ActiveNote) {
    listeners.forEach { it.onNoteActivated(note) }
  }

  private fun notifyNoteDeactivated(note: ActiveNote) {
    listeners.forEach { it.onNoteDeactivated(note) }
  }

  private fun notifyActiveNotesChanged() {
    val notesList = activeNotes.toList()
    listeners.forEach { it.onActiveNotesChanged(notesList) }
  }

  private fun notifySustainPedalStateChanged(isOn: Boolean) {
    listeners.forEach { it.onSustainPedalStateChanged(isOn) }
  }
} 
package com.midichords.model

import com.midichords.midi.MidiEvent
import com.midichords.midi.MidiEventListener

/**
 * Interface for processing MIDI notes and tracking active notes.
 */
interface NoteProcessor : MidiEventListener {
  /**
   * Get a list of all currently active notes.
   * @return List of active notes
   */
  fun getActiveNotes(): List<ActiveNote>
  
  /**
   * Check if a specific note is currently active.
   * @param noteNumber The MIDI note number to check
   * @param channel The MIDI channel to check (optional, defaults to -1 for any channel)
   * @return True if the note is active, false otherwise
   */
  fun isNoteActive(noteNumber: Int, channel: Int = -1): Boolean
  
  /**
   * Get the number of currently active notes.
   * @return The count of active notes
   */
  fun getActiveNoteCount(): Int
  
  /**
   * Set the sustain pedal state.
   * @param isOn True if the sustain pedal is pressed, false otherwise
   */
  fun setSustainPedal(isOn: Boolean)
  
  /**
   * Check if the sustain pedal is currently pressed.
   * @return True if the sustain pedal is pressed, false otherwise
   */
  fun isSustainPedalOn(): Boolean
  
  /**
   * Register a listener to be notified of note state changes.
   * @param listener The listener to register
   */
  fun registerNoteListener(listener: NoteStateListener)
  
  /**
   * Unregister a previously registered note state listener.
   * @param listener The listener to unregister
   */
  fun unregisterNoteListener(listener: NoteStateListener)
} 
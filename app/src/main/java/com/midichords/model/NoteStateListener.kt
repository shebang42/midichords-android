package com.midichords.model

/**
 * Interface for listening to note state changes.
 */
interface NoteStateListener {
  /**
   * Called when a note is activated (note on event).
   * @param note The activated note
   */
  fun onNoteActivated(note: ActiveNote)
  
  /**
   * Called when a note is deactivated (note off event).
   * @param note The deactivated note
   */
  fun onNoteDeactivated(note: ActiveNote)
  
  /**
   * Called when the collection of active notes changes.
   * @param activeNotes The current list of active notes
   */
  fun onActiveNotesChanged(activeNotes: List<ActiveNote>)
  
  /**
   * Called when the sustain pedal state changes.
   * @param isOn True if the sustain pedal is pressed, false otherwise
   */
  fun onSustainPedalStateChanged(isOn: Boolean)
} 
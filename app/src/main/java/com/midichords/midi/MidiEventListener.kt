package com.midichords.midi

/**
 * Interface for receiving MIDI events.
 */
interface MidiEventListener {
  /**
   * Called when a MIDI event is received.
   * @param event The MIDI event
   */
  fun onMidiEvent(event: MidiEvent)
} 
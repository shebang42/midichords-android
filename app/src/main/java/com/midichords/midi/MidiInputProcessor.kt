package com.midichords.midi

import android.media.midi.MidiReceiver

/**
 * Interface for processing MIDI input data and managing MIDI event listeners.
 */
interface MidiInputProcessor {
  /**
   * Process raw MIDI data and convert it to a MidiEvent.
   * @param data The raw MIDI data bytes
   * @param offset The offset in the data array where the MIDI message starts
   * @param length The length of the MIDI message in bytes
   * @param timestamp The timestamp of the MIDI message in nanoseconds
   * @return A MidiEvent if the data was successfully processed, null otherwise
   */
  fun processMidiData(data: ByteArray, offset: Int, length: Int, timestamp: Long): MidiEvent?

  /**
   * Get the MidiReceiver that can be connected to a MIDI input port.
   * @return The MidiReceiver instance
   */
  fun getReceiver(): MidiReceiver

  /**
   * Register a listener for MIDI events.
   * @param listener The listener to register
   */
  fun registerListener(listener: MidiEventListener)

  /**
   * Unregister a previously registered listener.
   * @param listener The listener to unregister
   */
  fun unregisterListener(listener: MidiEventListener)
} 
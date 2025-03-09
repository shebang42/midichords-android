package com.midichords.midi

import android.media.midi.MidiReceiver

/**
 * Interface for processing MIDI input data.
 */
interface MidiInputProcessor {
  /**
   * Process raw MIDI data.
   * @param data The MIDI data bytes
   * @param offset The offset in the data array where the message starts
   * @param length The length of the message in bytes
   * @param timestamp The timestamp of the message
   * @return The processed MidiEvent, or null if the data is invalid
   */
  fun processMidiData(data: ByteArray, offset: Int, length: Int, timestamp: Long): MidiEvent?

  /**
   * Get a MidiReceiver that will process incoming MIDI messages.
   * @return A MidiReceiver that forwards data to this processor
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
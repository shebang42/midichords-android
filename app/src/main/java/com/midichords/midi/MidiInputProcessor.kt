package com.midichords.midi

import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver

/**
 * Interface for processing MIDI input data
 */
interface MidiInputProcessor {
  /**
   * Get the MidiReceiver that can be connected to a MIDI input source
   */
  fun getReceiver(): MidiReceiver
  
  /**
   * Set the MIDI input port for this processor
   */
  fun setInputPort(inputPort: MidiInputPort)
  
  /**
   * Process raw MIDI data from a USB device
   * @return The last MIDI event processed, or null if no events were processed
   */
  fun processMidiData(data: ByteArray, offset: Int, count: Int, timestamp: Long): MidiEvent?
  
  /**
   * Register a listener to be notified of MIDI events
   */
  fun registerListener(listener: MidiEventListener)
  
  /**
   * Unregister a previously registered listener
   */
  fun unregisterListener(listener: MidiEventListener)
  
  /**
   * Clean up resources when the processor is no longer needed
   */
  fun cleanup()
} 
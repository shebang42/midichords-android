package com.midichords.midi

import android.media.midi.MidiReceiver
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of MidiInputProcessor that processes MIDI messages and notifies listeners.
 */
class MidiInputProcessorImpl : MidiInputProcessor {
  companion object {
    private const val TAG = "MidiInputProcessor"
  }

  private val listeners = CopyOnWriteArrayList<MidiEventListener>()
  
  private val midiReceiver = object : MidiReceiver() {
    override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
      processMidiData(data, offset, count, timestamp)?.let { event ->
        notifyListeners(event)
      }
    }
  }

  override fun processMidiData(data: ByteArray, offset: Int, length: Int, timestamp: Long): MidiEvent? {
    try {
      if (length < 1) return null
      
      val statusByte = data[offset].toInt() and 0xFF
      
      // Handle running status (when status byte is omitted)
      if (statusByte < 0x80) {
        Log.w(TAG, "Running status not supported")
        return null
      }

      // Get number of data bytes based on status
      val numDataBytes = when (statusByte and 0xF0) {
        0xC0, 0xD0 -> 1  // Program Change and Channel Pressure
        0xF0 -> when (statusByte) {
          0xF1, 0xF3 -> 1  // Time Code Quarter Frame, Song Select
          0xF2 -> 2  // Song Position Pointer
          else -> 0  // Other System messages
        }
        else -> 2  // All other messages (Note On/Off, Control Change, etc.)
      }

      // Check if we have enough data
      if (length < numDataBytes + 1) {
        Log.w(TAG, "Incomplete MIDI message")
        return null
      }

      // Extract data bytes
      val data1 = if (numDataBytes > 0) data[offset + 1].toInt() and 0xFF else 0
      val data2 = if (numDataBytes > 1) data[offset + 2].toInt() and 0xFF else 0

      return MidiEvent.fromBytes(statusByte, data1, data2)
    } catch (e: Exception) {
      Log.e(TAG, "Error processing MIDI data", e)
      return null
    }
  }

  override fun getReceiver(): MidiReceiver = midiReceiver

  override fun registerListener(listener: MidiEventListener) {
    listeners.addIfAbsent(listener)
  }

  override fun unregisterListener(listener: MidiEventListener) {
    listeners.remove(listener)
  }

  private fun notifyListeners(event: MidiEvent) {
    listeners.forEach { listener ->
      try {
        listener.onMidiEvent(event)
      } catch (e: Exception) {
        Log.e(TAG, "Error notifying listener", e)
      }
    }
  }
} 
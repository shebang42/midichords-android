package com.midichords.midi

import android.media.midi.MidiReceiver
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of MidiInputProcessor that processes MIDI messages and notifies listeners.
 */
class MidiInputProcessorImpl : MidiInputProcessor {
  companion object {
    private const val TAG = "MidiInputProcessorImpl"
  }

  private val listeners = CopyOnWriteArrayList<MidiEventListener>()
  private var lastStatusByte: Byte? = null

  private val midiReceiver = object : MidiReceiver() {
    override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
      val event = processMidiData(data, offset, count, timestamp)
      event?.let { notifyListeners(it) }
    }
  }

  override fun processMidiData(data: ByteArray, offset: Int, length: Int, timestamp: Long): MidiEvent? {
    if (length < 1) return null

    try {
      var currentOffset = offset
      val statusByte = data[currentOffset]

      // If this is a data byte and we have a running status, use the last status byte
      val actualStatusByte = if (statusByte < 0x80.toByte()) {
        lastStatusByte ?: return null // No running status available
      } else {
        currentOffset++ // Move past status byte
        statusByte.also { lastStatusByte = it }
      }

      // Get the remaining length after processing the status byte
      val remainingLength = length - (currentOffset - offset)
      if (remainingLength < 1) return null

      val command = (actualStatusByte.toInt() and 0xF0)
      val channel = (actualStatusByte.toInt() and 0x0F)

      return when (command) {
        0x80 -> { // Note Off
          if (remainingLength < 2) return null
          MidiEvent(
            type = MidiEventType.NOTE_OFF,
            channel = channel,
            data1 = data[currentOffset].toInt() and 0xFF,
            data2 = data[currentOffset + 1].toInt() and 0xFF,
            timestamp = timestamp
          )
        }
        0x90 -> { // Note On
          if (remainingLength < 2) return null
          val velocity = data[currentOffset + 1].toInt() and 0xFF
          MidiEvent(
            type = MidiEventType.NOTE_ON,
            channel = channel,
            data1 = data[currentOffset].toInt() and 0xFF,
            data2 = velocity,
            timestamp = timestamp
          )
        }
        0xB0 -> { // Control Change
          if (remainingLength < 2) return null
          MidiEvent(
            type = MidiEventType.CONTROL_CHANGE,
            channel = channel,
            data1 = data[currentOffset].toInt() and 0xFF,
            data2 = data[currentOffset + 1].toInt() and 0xFF,
            timestamp = timestamp
          )
        }
        else -> null // Unsupported command
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error processing MIDI data", e)
      return null
    }
  }

  override fun getReceiver(): MidiReceiver = midiReceiver

  override fun registerListener(listener: MidiEventListener) {
    listeners.add(listener)
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
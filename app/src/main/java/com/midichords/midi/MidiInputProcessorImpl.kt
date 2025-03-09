package com.midichords.midi

import android.media.midi.MidiInputPort
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
  private var currentInputPort: MidiInputPort? = null

  private val midiReceiver = object : MidiReceiver() {
    override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
      Log.d(TAG, "onSend received: ${count} bytes, offset: ${offset}, timestamp: ${timestamp}")
      // Log the raw bytes for debugging
      val hexData = data.slice(offset until offset + count).joinToString(" ") { 
        "0x${it.toInt().and(0xFF).toString(16).padStart(2, '0')}" 
      }
      Log.d(TAG, "Raw MIDI data: $hexData")
      
      val event = processMidiData(data, offset, count, timestamp)
      if (event != null) {
        Log.d(TAG, "Processed MIDI event: ${event.type}, channel: ${event.channel}, data1: ${event.data1}, data2: ${event.data2}")
        notifyListeners(event)
      } else {
        Log.w(TAG, "Failed to process MIDI data")
      }
    }
  }

  /**
   * Process raw MIDI data from a USB device
   * USB MIDI data format is typically 4 bytes per message:
   * Byte 0: Header (0x0n where n is the cable number)
   * Byte 1-3: MIDI message
   */
  override fun processMidiData(data: ByteArray, offset: Int, count: Int, timestamp: Long): MidiEvent? {
    try {
      Log.d(TAG, "Processing ${count} bytes of MIDI data")
      
      // Log the raw data for debugging
      val hexData = data.slice(offset until offset + count).joinToString(" ") { 
        "0x${it.toInt().and(0xFF).toString(16).padStart(2, '0')}" 
      }
      Log.d(TAG, "Raw MIDI data: $hexData")
      
      var latestEvent: MidiEvent? = null
      
      // Process USB MIDI packets (4 bytes each)
      var i = offset
      while (i < offset + count) {
        // Check if we have at least 4 bytes (USB MIDI packet)
        if (i + 3 < offset + count) {
          // USB MIDI packet format:
          // Byte 0: Header (0x0n where n is the cable number)
          // Byte 1-3: MIDI message
          
          val header = data[i].toInt() and 0xFF
          val status = data[i + 1].toInt() and 0xFF
          val data1 = data[i + 2].toInt() and 0xFF
          val data2 = data[i + 3].toInt() and 0xFF
          
          Log.d(TAG, "USB MIDI packet: Header=0x${header.toString(16)}, Status=0x${status.toString(16)}, Data1=0x${data1.toString(16)}, Data2=0x${data2.toString(16)}")
          
          // Extract the MIDI message
          val event = processMidiMessage(status, data1, data2, timestamp)
          if (event != null) {
            latestEvent = event
            // Dispatch the event to listeners
            notifyListeners(event)
          }
          
          i += 4 // Move to the next USB MIDI packet
        } else {
          // Not enough bytes for a complete USB MIDI packet
          Log.d(TAG, "Incomplete USB MIDI packet, skipping ${offset + count - i} bytes")
          break
        }
      }
      
      // Also try to process as regular MIDI data (not USB MIDI)
      if (latestEvent == null && count >= 2) {
        // Try to interpret as standard MIDI message
        val status = data[offset].toInt() and 0xFF
        val data1 = if (offset + 1 < offset + count) data[offset + 1].toInt() and 0xFF else 0
        val data2 = if (offset + 2 < offset + count) data[offset + 2].toInt() and 0xFF else 0
        
        Log.d(TAG, "Trying standard MIDI message: Status=0x${status.toString(16)}, Data1=0x${data1.toString(16)}, Data2=0x${data2.toString(16)}")
        
        val event = processMidiMessage(status, data1, data2, timestamp)
        if (event != null) {
          latestEvent = event
          // Dispatch the event to listeners
          notifyListeners(event)
        }
      }
      
      return latestEvent
    } catch (e: Exception) {
      Log.e(TAG, "Error processing MIDI data", e)
      return null
    }
  }

  /**
   * Process a single MIDI message
   */
  private fun processMidiMessage(status: Int, data1: Int, data2: Int, timestamp: Long): MidiEvent? {
    try {
      // Check if this is a status byte (MSB set)
      if (status < 0x80) {
        Log.d(TAG, "Not a status byte: 0x${status.toString(16)}")
        return null
      }
      
      val command = status and 0xF0
      val channel = status and 0x0F
      
      Log.d(TAG, "MIDI message: Command=0x${command.toString(16)}, Channel=$channel, Data1=$data1, Data2=$data2")
      
      return when (command) {
        0x80 -> { // Note Off
          Log.d(TAG, "Note Off: note=$data1, velocity=$data2")
          MidiEvent(
            type = MidiEventType.NOTE_OFF,
            channel = channel,
            data1 = data1,
            data2 = data2,
            timestamp = timestamp
          )
        }
        0x90 -> { // Note On
          // Note On with velocity 0 is equivalent to Note Off
          if (data2 == 0) {
            Log.d(TAG, "Note On with velocity 0 (treated as Note Off): note=$data1")
            MidiEvent(
              type = MidiEventType.NOTE_OFF,
              channel = channel,
              data1 = data1,
              data2 = 0,
              timestamp = timestamp
            )
          } else {
            Log.d(TAG, "Note On: note=$data1, velocity=$data2")
            MidiEvent(
              type = MidiEventType.NOTE_ON,
              channel = channel,
              data1 = data1,
              data2 = data2,
              timestamp = timestamp
            )
          }
        }
        0xB0 -> { // Control Change
          Log.d(TAG, "Control Change: controller=$data1, value=$data2")
          MidiEvent(
            type = MidiEventType.CONTROL_CHANGE,
            channel = channel,
            data1 = data1,
            data2 = data2,
            timestamp = timestamp
          )
        }
        else -> {
          Log.d(TAG, "Unsupported command: 0x${command.toString(16)}")
          null
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error processing MIDI message", e)
      return null
    }
  }

  override fun getReceiver(): MidiReceiver = midiReceiver

  override fun setInputPort(inputPort: MidiInputPort) {
    currentInputPort = inputPort
    Log.d(TAG, "MIDI input port set: $inputPort")
  }

  override fun registerListener(listener: MidiEventListener) {
    listeners.add(listener)
    Log.d(TAG, "Registered MIDI event listener: $listener, total listeners: ${listeners.size}")
  }

  override fun unregisterListener(listener: MidiEventListener) {
    listeners.remove(listener)
    Log.d(TAG, "Unregistered MIDI event listener: $listener, remaining listeners: ${listeners.size}")
  }

  private fun notifyListeners(event: MidiEvent) {
    Log.d(TAG, "Notifying ${listeners.size} listeners of MIDI event: ${event.type}")
    listeners.forEach { listener ->
      try {
        listener.onMidiEvent(event)
      } catch (e: Exception) {
        Log.e(TAG, "Error notifying listener: $listener", e)
      }
    }
  }
} 
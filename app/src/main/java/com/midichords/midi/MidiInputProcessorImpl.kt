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
   * Byte 0: Header (CIN and Cable Number)
   * Byte 1-3: MIDI message
   */
  override fun processMidiData(data: ByteArray, offset: Int, count: Int, timestamp: Long): MidiEvent? {
    try {
      if (count <= 0) {
        return null
      }
      
      Log.d(TAG, "Processing ${count} bytes of MIDI data")
      
      // Log the raw data for debugging (limit to first 16 bytes to avoid flooding logs)
      val logLimit = Math.min(count, 16)
      val hexData = data.slice(offset until offset + logLimit).joinToString(" ") { 
        "0x${it.toInt().and(0xFF).toString(16).padStart(2, '0')}" 
      }
      Log.d(TAG, "Raw MIDI data${if (count > 16) " (first 16 bytes)" else ""}: $hexData")
      
      var latestEvent: MidiEvent? = null
      
      // First, try to process as USB-MIDI packet format (4 bytes per message)
      // This is the most common format for USB MIDI devices
      if (count >= 4 && (count % 4 == 0)) {
        var i = offset
        var foundValidPacket = false
        
        while (i < offset + count) {
          // Make sure we have enough bytes for a complete packet
          if (i + 3 >= offset + count) break
          
          val header = data[i].toInt() and 0xFF
          val status = data[i + 1].toInt() and 0xFF
          val data1 = data[i + 2].toInt() and 0xFF
          val data2 = data[i + 3].toInt() and 0xFF
          
          // USB-MIDI header: 0x0n where n is the Code Index Number (CIN)
          // Only process if this looks like a valid USB-MIDI packet
          if ((header and 0xF0) == 0x00) {
            val cin = header and 0x0F
            // Check if this is a valid CIN for a MIDI message
            if (cin >= 0x08 && cin <= 0x0E) {
              foundValidPacket = true
              Log.d(TAG, "USB-MIDI packet: CIN=$cin, Status=0x${status.toString(16)}, Data1=0x${data1.toString(16)}, Data2=0x${data2.toString(16)}")
              
              // Process the message
              val event = processMidiMessage(status, data1, data2, timestamp)
              if (event != null) {
                latestEvent = event
                notifyListeners(event)
              }
            }
          }
          
          // Move to the next packet
          i += 4
        }
        
        // If we found at least one valid USB-MIDI packet, return the result
        if (foundValidPacket) {
          return latestEvent
        }
      }
      
      // If we couldn't process as USB-MIDI packets, try standard MIDI format
      // This is less common for USB devices but some might use it
      
      // Method 1: Try to process as single complete MIDI message
      if (count >= 2) {
        val status = data[offset].toInt() and 0xFF
        
        // Check if this starts with a status byte
        if (status >= 0x80) {
          val msgLength = getMidiMessageLength(status)
          
          if (offset + msgLength <= offset + count) {
            val data1 = if (msgLength > 1) data[offset + 1].toInt() and 0xFF else 0
            val data2 = if (msgLength > 2) data[offset + 2].toInt() and 0xFF else 0
            
            Log.d(TAG, "Standard MIDI message: Status=0x${status.toString(16)}, Data1=0x${data1.toString(16)}, Data2=0x${data2.toString(16)}")
            
            val event = processMidiMessage(status, data1, data2, timestamp)
            if (event != null) {
              latestEvent = event
              notifyListeners(event)
              return latestEvent
            }
          }
        }
      }
      
      // Method 2: Try to find a status byte and process from there
      var i = offset
      while (i < offset + count) {
        val b = data[i].toInt() and 0xFF
        if (b >= 0x80) { // Found a status byte
          val remaining = offset + count - i
          if (remaining >= 2) { // Need at least 2 bytes for a MIDI message
            val msgLength = getMidiMessageLength(b)
            if (i + msgLength <= offset + count) {
              val status = b
              val data1 = if (msgLength > 1) data[i + 1].toInt() and 0xFF else 0
              val data2 = if (msgLength > 2) data[i + 2].toInt() and 0xFF else 0
              
              Log.d(TAG, "Found embedded MIDI message: Status=0x${status.toString(16)}, Data1=0x${data1.toString(16)}, Data2=0x${data2.toString(16)}")
              
              val event = processMidiMessage(status, data1, data2, timestamp)
              if (event != null) {
                latestEvent = event
                notifyListeners(event)
                // Continue looking for more messages
              }
              
              // Skip ahead by the length of this message
              i += msgLength // Skip the entire message
              continue // Skip the increment at the end
            }
          }
        }
        i++ // Move to the next byte
      }
      
      return latestEvent
    } catch (e: Exception) {
      Log.e(TAG, "Error processing MIDI data", e)
      return null
    }
  }
  
  /**
   * Get the length of a MIDI message based on its status byte
   */
  private fun getMidiMessageLength(status: Int): Int {
    // Check if this is a status byte
    if (status < 0x80) {
      return 0 // Not a status byte
    }
    
    // Get the upper nibble (command)
    val command = status and 0xF0
    
    return when (command) {
      0x80, 0x90, 0xA0, 0xB0, 0xE0 -> 3 // Note Off, Note On, Poly Pressure, Control Change, Pitch Bend
      0xC0, 0xD0 -> 2 // Program Change, Channel Pressure
      0xF0 -> {
        // System messages
        when (status) {
          0xF1, 0xF3 -> 2 // MIDI Time Code, Song Select
          0xF2 -> 3 // Song Position Pointer
          0xF6, 0xF7, 0xF8, 0xFA, 0xFB, 0xFC, 0xFE, 0xFF -> 1 // Various system real-time messages
          else -> 1 // Default for unknown system messages
        }
      }
      else -> 1 // Default fallback
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
  
  /**
   * Clean up any resources when the processor is no longer needed
   */
  override fun cleanup() {
    Log.d(TAG, "Cleaning up MIDI input processor")
    // Close the input port if it's still open
    try {
      currentInputPort?.close()
      currentInputPort = null
    } catch (e: Exception) {
      Log.e(TAG, "Error closing MIDI input port during cleanup", e)
    }
    
    // Clear all listeners
    listeners.clear()
    
    // Clear any other state
    lastStatusByte = null
    
    Log.d(TAG, "MIDI input processor cleanup completed")
  }
} 
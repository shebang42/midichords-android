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

  override fun processMidiData(data: ByteArray, offset: Int, length: Int, timestamp: Long): MidiEvent? {
    if (length < 1) {
      Log.w(TAG, "processMidiData: Data length too short (${length})")
      return null
    }

    try {
      // Log the raw bytes for debugging
      val hexData = data.slice(offset until offset + length).joinToString(" ") { 
        "0x${it.toInt().and(0xFF).toString(16).padStart(2, '0')}" 
      }
      Log.d(TAG, "Processing MIDI data: $hexData")
      
      // Check for USB-MIDI format (4 bytes per message)
      if (length >= 4 && (data[offset].toInt() and 0xF0) == 0x00) {
        // This might be USB-MIDI format (4 bytes per message)
        // Byte 0: Cable Number (CN) and Code Index Number (CIN)
        // Bytes 1-3: MIDI data
        val cin = data[offset].toInt() and 0x0F
        
        // Check if this is a valid CIN
        if (cin >= 0x08 && cin <= 0x0E) {
          Log.d(TAG, "Detected USB-MIDI format, CIN: $cin")
          
          // Extract the MIDI data
          val statusByte = data[offset + 1]
          val data1 = data[offset + 2].toInt() and 0xFF
          val data2 = data[offset + 3].toInt() and 0xFF
          
          // Parse the status byte
          val command = statusByte.toInt() and 0xF0
          val channel = statusByte.toInt() and 0x0F
          
          Log.d(TAG, "USB-MIDI: Command: 0x${command.toString(16)}, Channel: $channel, Data1: $data1, Data2: $data2")
          
          return when (command) {
            0x80 -> { // Note Off
              Log.d(TAG, "USB-MIDI Note Off: note=$data1, velocity=$data2")
              MidiEvent(
                type = MidiEventType.NOTE_OFF,
                channel = channel,
                data1 = data1,
                data2 = data2,
                timestamp = timestamp
              )
            }
            0x90 -> { // Note On
              Log.d(TAG, "USB-MIDI Note On: note=$data1, velocity=$data2")
              MidiEvent(
                type = MidiEventType.NOTE_ON,
                channel = channel,
                data1 = data1,
                data2 = data2,
                timestamp = timestamp
              )
            }
            0xB0 -> { // Control Change
              Log.d(TAG, "USB-MIDI Control Change: controller=$data1, value=$data2")
              MidiEvent(
                type = MidiEventType.CONTROL_CHANGE,
                channel = channel,
                data1 = data1,
                data2 = data2,
                timestamp = timestamp
              )
            }
            else -> {
              Log.w(TAG, "Unsupported USB-MIDI command: 0x${command.toString(16)}")
              null
            }
          }
        }
      }
      
      // If not USB-MIDI format, try standard MIDI format
      var currentOffset = offset
      val statusByte = data[currentOffset]
      Log.d(TAG, "Processing status byte: 0x${statusByte.toInt().and(0xFF).toString(16)}")

      // If this is a data byte and we have a running status, use the last status byte
      val actualStatusByte = if (statusByte < 0x80.toByte()) {
        Log.d(TAG, "Using running status: ${lastStatusByte?.let { "0x${it.toInt().and(0xFF).toString(16)}" } ?: "none"}")
        lastStatusByte ?: run {
          Log.w(TAG, "No running status available for data byte")
          return null // No running status available
        }
      } else {
        currentOffset++ // Move past status byte
        statusByte.also { lastStatusByte = it }
      }

      // Get the remaining length after processing the status byte
      val remainingLength = length - (currentOffset - offset)
      if (remainingLength < 1) {
        Log.w(TAG, "Insufficient data after status byte")
        return null
      }

      val command = (actualStatusByte.toInt() and 0xF0)
      val channel = (actualStatusByte.toInt() and 0x0F)
      Log.d(TAG, "Command: 0x${command.toString(16)}, Channel: $channel")

      return when (command) {
        0x80 -> { // Note Off
          if (remainingLength < 2) {
            Log.w(TAG, "Insufficient data for Note Off message")
            return null
          }
          val note = data[currentOffset].toInt() and 0xFF
          val velocity = data[currentOffset + 1].toInt() and 0xFF
          Log.d(TAG, "Note Off: note=$note, velocity=$velocity")
          MidiEvent(
            type = MidiEventType.NOTE_OFF,
            channel = channel,
            data1 = note,
            data2 = velocity,
            timestamp = timestamp
          )
        }
        0x90 -> { // Note On
          if (remainingLength < 2) {
            Log.w(TAG, "Insufficient data for Note On message")
            return null
          }
          val note = data[currentOffset].toInt() and 0xFF
          val velocity = data[currentOffset + 1].toInt() and 0xFF
          Log.d(TAG, "Note On: note=$note, velocity=$velocity")
          MidiEvent(
            type = MidiEventType.NOTE_ON,
            channel = channel,
            data1 = note,
            data2 = velocity,
            timestamp = timestamp
          )
        }
        0xB0 -> { // Control Change
          if (remainingLength < 2) {
            Log.w(TAG, "Insufficient data for Control Change message")
            return null
          }
          val controller = data[currentOffset].toInt() and 0xFF
          val value = data[currentOffset + 1].toInt() and 0xFF
          Log.d(TAG, "Control Change: controller=$controller, value=$value")
          MidiEvent(
            type = MidiEventType.CONTROL_CHANGE,
            channel = channel,
            data1 = controller,
            data2 = value,
            timestamp = timestamp
          )
        }
        else -> {
          Log.w(TAG, "Unsupported command: 0x${command.toString(16)}")
          null // Unsupported command
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error processing MIDI data", e)
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
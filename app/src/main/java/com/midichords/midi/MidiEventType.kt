package com.midichords.midi

/**
 * Enumeration of MIDI message types.
 */
enum class MidiEventType(val statusByte: Int) {
  NOTE_OFF(0x80),
  NOTE_ON(0x90),
  POLYPHONIC_AFTERTOUCH(0xA0),
  CONTROL_CHANGE(0xB0),
  PROGRAM_CHANGE(0xC0),
  CHANNEL_AFTERTOUCH(0xD0),
  PITCH_BEND(0xE0),
  SYSTEM_EXCLUSIVE(0xF0),
  SYSTEM_COMMON(0xF1),
  SYSTEM_REAL_TIME(0xF8);

  companion object {
    /**
     * Get the event type from a status byte.
     * @param statusByte The status byte from the MIDI message
     * @return The corresponding MidiEventType or null if not found
     */
    fun fromStatusByte(statusByte: Int): MidiEventType? {
      val baseStatus = statusByte and 0xF0 // Mask off channel
      return values().find { it.statusByte == baseStatus }
    }
  }
} 
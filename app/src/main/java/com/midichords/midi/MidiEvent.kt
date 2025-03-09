package com.midichords.midi

/**
 * Represents a MIDI event with its type, channel, and data bytes.
 *
 * @property type The type of MIDI event
 * @property channel The MIDI channel (0-15)
 * @property data1 First data byte (note number for Note events, controller number for CC events)
 * @property data2 Second data byte (velocity for Note events, value for CC events)
 * @property timestamp The timestamp when the event was received
 */
data class MidiEvent(
  val type: MidiEventType,
  val channel: Int,
  val data1: Int,
  val data2: Int,
  val timestamp: Long = System.nanoTime()
) {
  companion object {
    /**
     * Create a MidiEvent from raw MIDI bytes.
     *
     * @param statusByte The status byte containing the event type and channel
     * @param data1 The first data byte
     * @param data2 The second data byte (optional for some message types)
     * @return A MidiEvent if the data is valid, null otherwise
     */
    fun fromBytes(statusByte: Int, data1: Int, data2: Int = 0): MidiEvent? {
      val type = MidiEventType.fromStatusByte(statusByte) ?: return null
      val channel = statusByte and 0x0F
      
      return MidiEvent(
        type = type,
        channel = channel,
        data1 = data1,
        data2 = data2
      )
    }
  }

  /**
   * Check if this is a Note On event with non-zero velocity.
   */
  fun isNoteOn(): Boolean = type == MidiEventType.NOTE_ON && data2 > 0

  /**
   * Check if this is a Note Off event or a Note On with zero velocity.
   */
  fun isNoteOff(): Boolean = type == MidiEventType.NOTE_OFF || (type == MidiEventType.NOTE_ON && data2 == 0)

  /**
   * Check if this is a sustain pedal event.
   */
  fun isSustainPedal(): Boolean = type == MidiEventType.CONTROL_CHANGE && data1 == 64

  /**
   * For sustain pedal events, returns true if the pedal is pressed.
   */
  fun isSustainOn(): Boolean = isSustainPedal() && data2 >= 64
} 
package com.midichords.model

/**
 * Represents an active MIDI note with its properties.
 *
 * @property noteNumber The MIDI note number (0-127)
 * @property velocity The velocity/intensity of the note (0-127)
 * @property channel The MIDI channel on which the note was received (0-15)
 * @property timestamp The timestamp when the note was activated (in milliseconds)
 * @property isSustained Whether this note is being sustained by the pedal
 */
data class ActiveNote(
  val noteNumber: Int,
  val velocity: Int,
  val channel: Int,
  val timestamp: Long,
  var isSustained: Boolean = false
) {
  /**
   * Returns the frequency in Hz for this note.
   * The formula is: frequency = 440 * 2^((noteNumber - 69) / 12)
   * where 69 is the note number for A4 (440 Hz)
   */
  fun getFrequency(): Double {
    return 440.0 * Math.pow(2.0, (noteNumber - 69).toDouble() / 12.0)
  }

  /**
   * Returns the note name with octave (e.g., "C4", "F#5")
   */
  fun getNoteName(): String {
    val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val noteName = noteNames[noteNumber % 12]
    val octave = (noteNumber / 12) - 1
    return "$noteName$octave"
  }

  /**
   * Returns true if this note is the same as another note (same note number and channel)
   */
  fun isSameNote(other: ActiveNote): Boolean {
    return noteNumber == other.noteNumber && channel == other.channel
  }

  companion object {
    // Constants for MIDI note numbers
    const val MIDDLE_C = 60
    const val A4 = 69
  }
} 
package com.midichords.model

/**
 * Represents a musical note with a pitch class and octave.
 *
 * @property pitchClass The pitch class of the note (C, C#, D, etc.)
 * @property octave The octave of the note (0-10)
 */
data class Note(
  val pitchClass: PitchClass,
  val octave: Int
) {
  /**
   * Get the MIDI note number for this note.
   * @return The MIDI note number (0-127)
   */
  fun toMidiNote(): Int {
    return (octave + 1) * 12 + pitchClass.position
  }

  /**
   * Get the frequency of this note in Hz.
   * @return The frequency in Hz
   */
  fun getFrequency(): Double {
    // A4 (MIDI note 69) is 440 Hz
    val midiNote = toMidiNote()
    return 440.0 * Math.pow(2.0, (midiNote - 69).toDouble() / 12.0)
  }

  /**
   * Get the name of this note including the octave.
   * @param useFlats Whether to use flat notation instead of sharp notation
   * @return The name of the note (e.g., "C4", "F#5")
   */
  fun getName(useFlats: Boolean = false): String {
    return "${pitchClass.getName(useFlats)}$octave"
  }

  companion object {
    /**
     * Create a Note from a MIDI note number.
     * @param midiNote The MIDI note number (0-127)
     * @return The corresponding Note
     */
    fun fromMidiNote(midiNote: Int): Note {
      val pitchClass = PitchClass.fromMidiNote(midiNote)
      val octave = (midiNote / 12) - 1
      return Note(pitchClass, octave)
    }

    // Common note constants
    val MIDDLE_C = fromMidiNote(60)
    val A4 = fromMidiNote(69)
  }
} 
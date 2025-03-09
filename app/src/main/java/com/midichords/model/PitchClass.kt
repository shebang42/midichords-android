package com.midichords.model

/**
 * Enum representing the 12 pitch classes in Western music.
 * Each pitch class has a name and can be represented with sharps or flats.
 */
enum class PitchClass(
  val position: Int,
  val sharpName: String,
  val flatName: String
) {
  C(0, "C", "C"),
  C_SHARP(1, "C#", "Db"),
  D(2, "D", "D"),
  D_SHARP(3, "D#", "Eb"),
  E(4, "E", "E"),
  F(5, "F", "F"),
  F_SHARP(6, "F#", "Gb"),
  G(7, "G", "G"),
  G_SHARP(8, "G#", "Ab"),
  A(9, "A", "A"),
  A_SHARP(10, "A#", "Bb"),
  B(11, "B", "B");

  /**
   * Get the name of the pitch class using the specified notation.
   * @param useFlats Whether to use flat notation instead of sharp notation
   * @return The name of the pitch class
   */
  fun getName(useFlats: Boolean = false): String {
    return if (useFlats) flatName else sharpName
  }

  companion object {
    /**
     * Get a PitchClass from a MIDI note number.
     * @param midiNote The MIDI note number (0-127)
     * @return The corresponding PitchClass
     */
    fun fromMidiNote(midiNote: Int): PitchClass {
      return values()[midiNote % 12]
    }

    /**
     * Get a PitchClass from its position (0-11).
     * @param position The position in the chromatic scale (0 = C, 1 = C#, etc.)
     * @return The corresponding PitchClass
     */
    fun fromPosition(position: Int): PitchClass {
      return values()[position % 12]
    }
  }
} 
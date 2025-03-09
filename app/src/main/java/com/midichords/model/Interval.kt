package com.midichords.model

/**
 * Enum representing common musical intervals.
 * Each interval has a name, semitone count, and quality.
 */
enum class Interval(
  val semitones: Int,
  val shortName: String,
  val longName: String
) {
  UNISON(0, "P1", "Perfect Unison"),
  MINOR_SECOND(1, "m2", "Minor Second"),
  MAJOR_SECOND(2, "M2", "Major Second"),
  MINOR_THIRD(3, "m3", "Minor Third"),
  MAJOR_THIRD(4, "M3", "Major Third"),
  PERFECT_FOURTH(5, "P4", "Perfect Fourth"),
  TRITONE(6, "TT", "Tritone"),
  PERFECT_FIFTH(7, "P5", "Perfect Fifth"),
  MINOR_SIXTH(8, "m6", "Minor Sixth"),
  MAJOR_SIXTH(9, "M6", "Major Sixth"),
  MINOR_SEVENTH(10, "m7", "Minor Seventh"),
  MAJOR_SEVENTH(11, "M7", "Major Seventh"),
  OCTAVE(12, "P8", "Perfect Octave");

  companion object {
    /**
     * Get an Interval from a semitone count.
     * @param semitones The number of semitones
     * @return The corresponding Interval, or null if no exact match
     */
    fun fromSemitones(semitones: Int): Interval? {
      return values().find { it.semitones == semitones % 12 }
    }

    /**
     * Calculate the interval between two pitch classes.
     * @param from The starting pitch class
     * @param to The ending pitch class
     * @return The interval between the pitch classes
     */
    fun between(from: PitchClass, to: PitchClass): Interval? {
      val semitones = (to.position - from.position + 12) % 12
      return fromSemitones(semitones)
    }

    /**
     * Calculate the interval between two notes.
     * @param from The starting note
     * @param to The ending note
     * @return The interval between the notes (ignoring octaves)
     */
    fun between(from: Note, to: Note): Interval? {
      return between(from.pitchClass, to.pitchClass)
    }

    /**
     * Calculate the exact interval in semitones between two notes.
     * @param from The starting note
     * @param to The ending note
     * @return The exact interval in semitones (can be negative)
     */
    fun semitonesBetween(from: Note, to: Note): Int {
      return to.toMidiNote() - from.toMidiNote()
    }
  }
} 
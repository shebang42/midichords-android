package com.midichords.model

/**
 * Enum representing different types of chords.
 * Each chord type has a name, symbol, and a list of intervals that define it.
 */
enum class ChordType(
  val fullName: String,
  val symbol: String,
  val intervals: List<Int>
) {
  // Triads
  MAJOR("Major", "", listOf(0, 4, 7)),
  MINOR("Minor", "m", listOf(0, 3, 7)),
  DIMINISHED("Diminished", "dim", listOf(0, 3, 6)),
  AUGMENTED("Augmented", "aug", listOf(0, 4, 8)),
  SUSPENDED_2("Suspended 2nd", "sus2", listOf(0, 2, 7)),
  SUSPENDED_4("Suspended 4th", "sus4", listOf(0, 5, 7)),

  // Seventh chords
  DOMINANT_7TH("Dominant 7th", "7", listOf(0, 4, 7, 10)),
  MAJOR_7TH("Major 7th", "maj7", listOf(0, 4, 7, 11)),
  MINOR_7TH("Minor 7th", "m7", listOf(0, 3, 7, 10)),
  MINOR_MAJOR_7TH("Minor Major 7th", "mMaj7", listOf(0, 3, 7, 11)),
  DIMINISHED_7TH("Diminished 7th", "dim7", listOf(0, 3, 6, 9)),
  HALF_DIMINISHED_7TH("Half Diminished 7th", "m7b5", listOf(0, 3, 6, 10)),
  AUGMENTED_7TH("Augmented 7th", "aug7", listOf(0, 4, 8, 10)),
  AUGMENTED_MAJOR_7TH("Augmented Major 7th", "augMaj7", listOf(0, 4, 8, 11)),

  // Extended chords
  DOMINANT_9TH("Dominant 9th", "9", listOf(0, 4, 7, 10, 14)),
  MAJOR_9TH("Major 9th", "maj9", listOf(0, 4, 7, 11, 14)),
  MINOR_9TH("Minor 9th", "m9", listOf(0, 3, 7, 10, 14)),
  DOMINANT_11TH("Dominant 11th", "11", listOf(0, 4, 7, 10, 14, 17)),
  MAJOR_11TH("Major 11th", "maj11", listOf(0, 4, 7, 11, 14, 17)),
  MINOR_11TH("Minor 11th", "m11", listOf(0, 3, 7, 10, 14, 17)),
  DOMINANT_13TH("Dominant 13th", "13", listOf(0, 4, 7, 10, 14, 17, 21)),
  MAJOR_13TH("Major 13th", "maj13", listOf(0, 4, 7, 11, 14, 17, 21)),
  MINOR_13TH("Minor 13th", "m13", listOf(0, 3, 7, 10, 14, 17, 21)),

  // Other common chords
  ADDED_9TH("Added 9th", "add9", listOf(0, 4, 7, 14)),
  SIXTH("Sixth", "6", listOf(0, 4, 7, 9)),
  MINOR_SIXTH("Minor Sixth", "m6", listOf(0, 3, 7, 9)),
  SIXTH_NINTH("Sixth/Ninth", "6/9", listOf(0, 4, 7, 9, 14)),
  SEVENTH_FLAT_5("Seventh Flat 5", "7b5", listOf(0, 4, 6, 10)),
  SEVENTH_SHARP_5("Seventh Sharp 5", "7#5", listOf(0, 4, 8, 10)),
  NINTH_FLAT_5("Ninth Flat 5", "9b5", listOf(0, 4, 6, 10, 14)),
  NINTH_SHARP_5("Ninth Sharp 5", "9#5", listOf(0, 4, 8, 10, 14)),
  SEVENTH_FLAT_9("Seventh Flat 9", "7b9", listOf(0, 4, 7, 10, 13)),
  SEVENTH_SHARP_9("Seventh Sharp 9", "7#9", listOf(0, 4, 7, 10, 15)),
  SEVENTH_FLAT_5_FLAT_9("Seventh Flat 5 Flat 9", "7b5b9", listOf(0, 4, 6, 10, 13)),
  SEVENTH_SHARP_5_FLAT_9("Seventh Sharp 5 Flat 9", "7#5b9", listOf(0, 4, 8, 10, 13)),
  SEVENTH_FLAT_5_SHARP_9("Seventh Flat 5 Sharp 9", "7b5#9", listOf(0, 4, 6, 10, 15)),
  SEVENTH_SHARP_5_SHARP_9("Seventh Sharp 5 Sharp 9", "7#5#9", listOf(0, 4, 8, 10, 15));

  companion object {
    /**
     * Find a chord type that matches the given intervals.
     * @param intervals The intervals to match
     * @return The matching chord type, or null if no match is found
     */
    fun findByIntervals(intervals: List<Int>): ChordType? {
      // Normalize intervals to start from 0
      val normalizedIntervals = if (intervals.isNotEmpty() && intervals[0] != 0) {
        val root = intervals[0]
        intervals.map { (it - root + 12) % 12 }.sorted()
      } else {
        intervals.sorted()
      }

      return values().find { it.intervals == normalizedIntervals }
    }
  }
} 
package com.midichords.model

/**
 * Represents a musical scale with a root note and a pattern of intervals.
 *
 * @property root The root note of the scale
 * @property intervals The intervals that define the scale
 * @property name The name of the scale
 */
class Scale(
  val root: PitchClass,
  val intervals: List<Int>,
  val name: String
) {
  /**
   * Get the pitch classes in this scale.
   * @return List of pitch classes in the scale
   */
  fun getPitchClasses(): List<PitchClass> {
    return intervals.map { interval ->
      val position = (root.position + interval) % 12
      PitchClass.fromPosition(position)
    }
  }

  /**
   * Get the notes in this scale for a specific octave.
   * @param octave The octave to generate notes for
   * @return List of notes in the scale
   */
  fun getNotes(octave: Int): List<Note> {
    val notes = mutableListOf<Note>()
    var currentOctave = octave

    intervals.forEach { interval ->
      val position = (root.position + interval) % 12
      val octaveShift = (root.position + interval) / 12
      val noteOctave = currentOctave + octaveShift
      notes.add(Note(PitchClass.fromPosition(position), noteOctave))
    }

    return notes
  }

  /**
   * Check if a pitch class is in this scale.
   * @param pitchClass The pitch class to check
   * @return True if the pitch class is in the scale, false otherwise
   */
  fun contains(pitchClass: PitchClass): Boolean {
    return getPitchClasses().contains(pitchClass)
  }

  /**
   * Check if a note is in this scale (ignoring octave).
   * @param note The note to check
   * @return True if the note's pitch class is in the scale, false otherwise
   */
  fun contains(note: Note): Boolean {
    return contains(note.pitchClass)
  }

  companion object {
    // Common scale interval patterns
    private val MAJOR_SCALE_INTERVALS = listOf(0, 2, 4, 5, 7, 9, 11)
    private val NATURAL_MINOR_SCALE_INTERVALS = listOf(0, 2, 3, 5, 7, 8, 10)
    private val HARMONIC_MINOR_SCALE_INTERVALS = listOf(0, 2, 3, 5, 7, 8, 11)
    private val MELODIC_MINOR_SCALE_INTERVALS = listOf(0, 2, 3, 5, 7, 9, 11)
    private val PENTATONIC_MAJOR_INTERVALS = listOf(0, 2, 4, 7, 9)
    private val PENTATONIC_MINOR_INTERVALS = listOf(0, 3, 5, 7, 10)
    private val BLUES_SCALE_INTERVALS = listOf(0, 3, 5, 6, 7, 10)

    /**
     * Create a major scale with the specified root.
     * @param root The root pitch class
     * @return A major scale
     */
    fun major(root: PitchClass): Scale {
      return Scale(root, MAJOR_SCALE_INTERVALS, "${root.sharpName} Major")
    }

    /**
     * Create a natural minor scale with the specified root.
     * @param root The root pitch class
     * @return A natural minor scale
     */
    fun naturalMinor(root: PitchClass): Scale {
      return Scale(root, NATURAL_MINOR_SCALE_INTERVALS, "${root.sharpName} Minor")
    }

    /**
     * Create a harmonic minor scale with the specified root.
     * @param root The root pitch class
     * @return A harmonic minor scale
     */
    fun harmonicMinor(root: PitchClass): Scale {
      return Scale(root, HARMONIC_MINOR_SCALE_INTERVALS, "${root.sharpName} Harmonic Minor")
    }

    /**
     * Create a melodic minor scale with the specified root.
     * @param root The root pitch class
     * @return A melodic minor scale
     */
    fun melodicMinor(root: PitchClass): Scale {
      return Scale(root, MELODIC_MINOR_SCALE_INTERVALS, "${root.sharpName} Melodic Minor")
    }

    /**
     * Create a major pentatonic scale with the specified root.
     * @param root The root pitch class
     * @return A major pentatonic scale
     */
    fun majorPentatonic(root: PitchClass): Scale {
      return Scale(root, PENTATONIC_MAJOR_INTERVALS, "${root.sharpName} Major Pentatonic")
    }

    /**
     * Create a minor pentatonic scale with the specified root.
     * @param root The root pitch class
     * @return A minor pentatonic scale
     */
    fun minorPentatonic(root: PitchClass): Scale {
      return Scale(root, PENTATONIC_MINOR_INTERVALS, "${root.sharpName} Minor Pentatonic")
    }

    /**
     * Create a blues scale with the specified root.
     * @param root The root pitch class
     * @return A blues scale
     */
    fun blues(root: PitchClass): Scale {
      return Scale(root, BLUES_SCALE_INTERVALS, "${root.sharpName} Blues")
    }
  }
} 
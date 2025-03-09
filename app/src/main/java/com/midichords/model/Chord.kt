package com.midichords.model

/**
 * Represents a musical chord with a root note, chord type, and inversion.
 *
 * @property root The root note of the chord
 * @property type The type of chord (e.g., major, minor, dominant 7th)
 * @property inversion The inversion of the chord (0 = root position, 1 = first inversion, etc.)
 * @property bassNote The bass note of the chord (lowest note, may differ from root in inversions)
 */
data class Chord(
  val root: PitchClass,
  val type: ChordType,
  val inversion: Int = 0,
  val bassNote: PitchClass? = null
) {
  /**
   * Get the intervals that define this chord.
   * @return List of intervals from the root
   */
  fun getIntervals(): List<Int> {
    return type.intervals
  }

  /**
   * Get the pitch classes in this chord.
   * @return List of pitch classes in the chord
   */
  fun getPitchClasses(): List<PitchClass> {
    return type.intervals.map { interval ->
      val position = (root.position + interval) % 12
      PitchClass.fromPosition(position)
    }
  }

  /**
   * Get the name of this chord.
   * @param useFlats Whether to use flat notation instead of sharp notation
   * @return The formatted chord name (e.g., "C", "Dm7", "G7/B")
   */
  fun getName(useFlats: Boolean = false): String {
    val rootName = root.getName(useFlats)
    val typeSymbol = type.getSymbol()
    
    return if (bassNote != null && bassNote != root) {
      // Include the bass note for slash chords
      "$rootName$typeSymbol/${bassNote.getName(useFlats)}"
    } else {
      "$rootName$typeSymbol"
    }
  }

  /**
   * Get the full name of this chord.
   * @param useFlats Whether to use flat notation instead of sharp notation
   * @return The full chord name (e.g., "C Major", "D Minor 7th", "G Dominant 7th/B")
   */
  fun getFullName(useFlats: Boolean = false): String {
    val rootName = root.getName(useFlats)
    val typeName = type.getFormattedName()
    
    val inversionText = when (inversion) {
      0 -> ""
      1 -> " (1st inversion)"
      2 -> " (2nd inversion)"
      3 -> " (3rd inversion)"
      else -> " (${inversion}th inversion)"
    }
    
    return if (bassNote != null && bassNote != root) {
      // Include the bass note for slash chords
      "$rootName $typeName/$bassNote$inversionText"
    } else {
      "$rootName $typeName$inversionText"
    }
  }

  /**
   * Check if this chord contains a specific pitch class.
   * @param pitchClass The pitch class to check
   * @return True if the chord contains the pitch class, false otherwise
   */
  fun contains(pitchClass: PitchClass): Boolean {
    return getPitchClasses().contains(pitchClass)
  }

  /**
   * Create a new chord with a different inversion.
   * @param newInversion The new inversion value
   * @return A new Chord with the specified inversion
   */
  fun withInversion(newInversion: Int): Chord {
    if (newInversion < 0 || newInversion >= type.intervals.size) {
      throw IllegalArgumentException("Invalid inversion: $newInversion")
    }
    
    val pitchClasses = getPitchClasses()
    val newBassNote = if (newInversion > 0 && pitchClasses.size > newInversion) {
      pitchClasses[newInversion]
    } else {
      root
    }
    
    return Chord(root, type, newInversion, newBassNote)
  }

  companion object {
    /**
     * Create a major chord with the specified root.
     * @param root The root pitch class
     * @return A major chord
     */
    fun major(root: PitchClass): Chord {
      return Chord(root, ChordType.MAJOR)
    }

    /**
     * Create a minor chord with the specified root.
     * @param root The root pitch class
     * @return A minor chord
     */
    fun minor(root: PitchClass): Chord {
      return Chord(root, ChordType.MINOR)
    }

    /**
     * Create a dominant 7th chord with the specified root.
     * @param root The root pitch class
     * @return A dominant 7th chord
     */
    fun dominant7(root: PitchClass): Chord {
      return Chord(root, ChordType.DOMINANT_7TH)
    }

    /**
     * Create a major 7th chord with the specified root.
     * @param root The root pitch class
     * @return A major 7th chord
     */
    fun major7(root: PitchClass): Chord {
      return Chord(root, ChordType.MAJOR_7TH)
    }

    /**
     * Create a minor 7th chord with the specified root.
     * @param root The root pitch class
     * @return A minor 7th chord
     */
    fun minor7(root: PitchClass): Chord {
      return Chord(root, ChordType.MINOR_7TH)
    }
  }
} 
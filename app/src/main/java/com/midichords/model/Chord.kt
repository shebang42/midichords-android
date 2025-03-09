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
    val typeSymbol = type.symbol
    
    return if (bassNote != null && bassNote != root) {
      // Include the bass note for slash chords
      "$rootName$typeSymbol/${bassNote.getName(useFlats)}"
    } else {
      "$rootName$typeSymbol"
    }
  }

  /**
   * Get alternative name for this chord, if available.
   * Some chords can be interpreted in multiple ways.
   * @param useFlats Whether to use flat notation instead of sharp notation
   * @return Alternative chord name or null if no alternative exists
   */
  fun getAlternativeName(useFlats: Boolean = false): String? {
    // Define rootName for use within this method
    val rootName = root.getName(useFlats)
    
    // Check for common alternative interpretations
    return when {
      // C/E can also be seen as Em7/C in some contexts
      type == ChordType.MAJOR && inversion == 1 -> {
        val thirdAlt = PitchClass.fromPosition((root.position + 4) % 12)
        "${thirdAlt.getName(useFlats)}m7/${root.getName(useFlats)}"
      }
      
      // C/G can sometimes be interpreted as G11(no3,5,9)
      type == ChordType.MAJOR && inversion == 2 -> {
        val fifthAlt = PitchClass.fromPosition((root.position + 7) % 12)
        "${fifthAlt.getName(useFlats)}sus4(add7)"
      }
      
      // Cmaj7 can be viewed as Em/C
      type == ChordType.MAJOR_7TH && inversion == 0 -> {
        val relativeMinor = PitchClass.fromPosition((root.position + 9) % 12)
        "${relativeMinor.getName(useFlats)}m/${root.getName(useFlats)}"
      }
      
      // C7 can be seen as Edim/Bb in some contexts
      type == ChordType.DOMINANT_7TH && inversion == 3 -> {
        val thirdNote = PitchClass.fromPosition((root.position + 4) % 12)
        "${thirdNote.getName(useFlats)}dim7(no5)"
      }
      
      // Cm7b5 (half-diminished) is the same as Abmaj7/C
      type == ChordType.HALF_DIMINISHED_7TH -> {
        val flatSixth = PitchClass.fromPosition((root.position + 8) % 12)
        "${flatSixth.getName(useFlats)}maj7/${root.getName(useFlats)}"
      }
      
      // G7#11 can be interpreted as D7alt/G
      type == ChordType.DOMINANT_7_SHARP_11 -> {
        val fifth = PitchClass.fromPosition((root.position + 7) % 12)
        "${fifth.getName(useFlats)}7alt/${root.getName(useFlats)}"
      }
      
      // For add9 chords, can be seen as sus2 with added 3rd
      type == ChordType.ADDED_9TH -> {
        "$rootName${if (useFlats) "sus2(add3)" else "sus2(add3)"}"
      }
      
      // For quartal chord, can show as stacked 4ths
      type == ChordType.QUARTAL -> {
        val fourthUp = PitchClass.fromPosition((root.position + 5) % 12)
        "${root.getName(useFlats)}-${fourthUp.getName(useFlats)}-${bassNote?.getName(useFlats) ?: "?"} (4ths)"
      }
      
      else -> null
    }
  }

  /**
   * Get the full name of this chord.
   * @param useFlats Whether to use flat notation instead of sharp notation
   * @return The full chord name (e.g., "C Major", "D Minor 7th", "G Dominant 7th/B")
   */
  fun getFullName(useFlats: Boolean = false): String {
    val rootName = root.getName(useFlats)
    val typeName = type.fullName
    
    val inversionText = when (inversion) {
      0 -> ""
      1 -> " (1st inversion)"
      2 -> " (2nd inversion)"
      3 -> " (3rd inversion)"
      else -> " (${inversion}th inversion)"
    }
    
    return if (bassNote != null && bassNote != root) {
      // Include the bass note for slash chords
      "$rootName $typeName/${bassNote.getName(useFlats)}$inversionText"
    } else {
      "$rootName $typeName$inversionText"
    }
  }

  /**
   * Get jazz-oriented name of this chord with extended notation where applicable
   * @param useFlats Whether to use flat notation instead of sharp notation
   * @return The jazz chord name with extended notation
   */
  fun getJazzName(useFlats: Boolean = false): String {
    val rootName = root.getName(useFlats)
    
    // Customize names for certain jazz chord types
    val jazzSymbol = when (type) {
      ChordType.ALTERED -> "7alt"
      ChordType.LYDIAN_DOMINANT -> "7#11"
      ChordType.PHRYGIAN -> "7sus4♭9"
      ChordType.DOMINANT_13_FLAT_9_SHARP_11 -> "13♭9#11"
      ChordType.HALF_DIMINISHED_7TH -> "ø"
      ChordType.DOMINANT_7_SHARP_11 -> "7#11"
      ChordType.DOMINANT_7_FLAT_13 -> "7♭13"
      ChordType.QUARTAL -> "4ths"
      ChordType.SO_WHAT -> "so what"
      ChordType.DOMINANT_13_FLAT_9 -> "13♭9"
      ChordType.DOMINANT_13_SHARP_9 -> "13#9"
      else -> type.symbol
    }
    
    return if (bassNote != null && bassNote != root) {
      // Include the bass note for slash chords
      "$rootName$jazzSymbol/${bassNote.getName(useFlats)}"
    } else {
      "$rootName$jazzSymbol"
    }
  }

  /**
   * Get the most common voicing of this chord as a list of pitch classes
   * in the order they would typically be played.
   * @return List of pitch classes in voicing order
   */
  fun getVoicing(): List<PitchClass> {
    val pitchClasses = getPitchClasses()
    
    // For inversions, start with the bass note
    return if (inversion > 0 && inversion < pitchClasses.size) {
      // Reorder pitch classes starting with the bass note
      val invertedClasses = pitchClasses.subList(inversion, pitchClasses.size) +
                           pitchClasses.subList(0, inversion)
      invertedClasses
    } else {
      pitchClasses
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

  /**
   * Create a new chord with an explicit bass note, creating a slash chord.
   * @param bass The bass note to use
   * @return A new Chord with the specified bass note
   */
  fun withBassNote(bass: PitchClass): Chord {
    if (bass == root) {
      return this.copy(inversion = 0, bassNote = root)
    }
    
    // Try to find the bass note in the chord to determine inversion
    val pitchClasses = getPitchClasses()
    val inversionIndex = pitchClasses.indexOf(bass)
    
    return if (inversionIndex > 0) {
      // This is a standard inversion
      this.copy(inversion = inversionIndex, bassNote = bass)
    } else {
      // This is a non-standard slash chord (bass note not in chord)
      this.copy(bassNote = bass)
    }
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

    /**
     * Create a slash chord with the specified root chord and bass note.
     * @param rootChord The root chord
     * @param bassNote The bass note
     * @return A slash chord
     */
    fun slashChord(rootChord: Chord, bassNote: PitchClass): Chord {
      return rootChord.withBassNote(bassNote)
    }
    
    /**
     * Create an altered dominant chord with the specified root.
     * @param root The root pitch class
     * @return An altered dominant chord
     */
    fun altered(root: PitchClass): Chord {
      return Chord(root, ChordType.ALTERED)
    }
  }
} 
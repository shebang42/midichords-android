package com.midichords.model

import android.util.Log

/**
 * Advanced implementation of the ChordIdentifier interface that identifies
 * complex chords and handles voicings with better accuracy.
 */
class AdvancedChordIdentifier : BasicChordIdentifier() {
  companion object {
    private const val TAG = "AdvancedChordIdentifier"
    private const val CHORD_MATCH_THRESHOLD = 0.8 // 80% of notes must match for a chord type
  }

  /**
   * Enhance chord identification by analyzing the context of the notes
   * and applying more sophisticated algorithms for complex chords.
   */
  override fun identifyChordFromPitchClasses(pitchClasses: List<PitchClass>): Chord? {
    // First try the basic algorithm
    val basicChord = super.identifyChordFromPitchClasses(pitchClasses)
    if (basicChord != null) {
      return basicChord
    }

    // If basic algorithm failed, try with partial matching
    if (pitchClasses.size >= 3) {
      // Try all possible roots and chord types with partial matching
      return findBestPartialMatch(pitchClasses)
    }

    return null
  }

  /**
   * Find the best partial match when exact chord type matching fails.
   * This is useful for identifying chords when some notes are missing or added.
   */
  private fun findBestPartialMatch(pitchClasses: List<PitchClass>): Chord? {
    var bestMatch: Chord? = null
    var bestMatchScore = 0.0
    
    // Try each pitch class as potential root
    for (root in pitchClasses) {
      // Calculate intervals from the root
      val intervals = pitchClasses.map { pc ->
        (pc.position - root.position + 12) % 12
      }.sorted().distinct()
      
      // Ensure root is included
      if (!intervals.contains(0)) {
        continue
      }
      
      // Try each chord type for partial matching
      for (chordType in ChordType.values()) {
        val score = calculateMatchScore(intervals, chordType.intervals)
        if (score > bestMatchScore && score >= CHORD_MATCH_THRESHOLD) {
          bestMatchScore = score
          
          // Determine inversion based on bass note
          val bassNote = findBassNote()
          var inversion = 0
          var bassPC: PitchClass? = null
          
          if (bassNote != null) {
            bassPC = Note.fromMidiNote(bassNote.noteNumber).pitchClass
            val chordPCs = chordType.intervals.map { interval ->
              val position = (root.position + interval) % 12
              PitchClass.fromPosition(position)
            }
            
            inversion = chordPCs.indexOf(bassPC)
            if (inversion < 0) {
              // Bass note is not in the chord, it's a slash chord
              inversion = 0
            }
          }
          
          bestMatch = Chord(root, chordType, inversion, bassPC)
        }
      }
    }
    
    if (bestMatch != null) {
      Log.d(TAG, "Found partial match chord: ${bestMatch.getName()} with score: $bestMatchScore")
    }
    
    return bestMatch
  }

  /**
   * Calculate a match score between the actual intervals and a chord type's intervals.
   * This allows for partial matching when some notes are missing or extra notes are added.
   * @return A score between 0.0 and 1.0, where 1.0 is a perfect match
   */
  private fun calculateMatchScore(actualIntervals: List<Int>, chordIntervals: List<Int>): Double {
    val intersection = actualIntervals.intersect(chordIntervals.toSet())
    
    // Calculate precision and recall
    val precision = intersection.size.toDouble() / actualIntervals.size
    val recall = intersection.size.toDouble() / chordIntervals.size
    
    // F1 score (harmonic mean of precision and recall)
    return if (precision + recall > 0) {
      2 * (precision * recall) / (precision + recall)
    } else {
      0.0
    }
  }
  
  /**
   * Find the bass note (lowest note) from the current active notes.
   */
  private fun findBassNote(): ActiveNote? {
    if (currentNotes.isEmpty()) {
      return null
    }
    
    // Find the note with the lowest MIDI note number
    return currentNotes.minByOrNull { it.noteNumber }
  }
} 
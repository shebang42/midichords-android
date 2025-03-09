package com.midichords.view

import android.graphics.Canvas
import android.graphics.Paint
import com.midichords.model.ActiveNote

/**
 * Helper class for rendering notes on a musical staff.
 * Handles calculations for note positioning and rendering.
 */
class NoteRenderer {

  companion object {
    // MIDI note number for middle C
    private const val MIDDLE_C = 60
    
    // Line position for middle C (0-indexed from top of staff)
    private const val MIDDLE_C_LINE = 10
    
    // White keys in the octave (C, D, E, F, G, A, B)
    private val WHITE_KEYS = setOf(0, 2, 4, 5, 7, 9, 11)
    
    // Black keys in the octave (C#, D#, F#, G#, A#)
    private val BLACK_KEYS = setOf(1, 3, 6, 8, 10)
    
    // Map note numbers to accidentals (simplified to just sharp and flat)
    private val NOTE_TO_ACCIDENTAL = mapOf(
      1 to Accidental.SHARP,  // C#
      3 to Accidental.SHARP,  // D#
      6 to Accidental.SHARP,  // F#
      8 to Accidental.SHARP,  // G#
      10 to Accidental.SHARP  // A#
    )
  }

  /**
   * Calculates the staff line position for a given MIDI note number.
   * Returns a line index where:
   * - Even numbers are lines
   * - Odd numbers are spaces between lines
   * - Middle C is at line index 10
   * - Lines go from top to bottom, so lower index = higher on the staff
   *
   * @param midiNote MIDI note number (0-127)
   * @return Line position (0-indexed from top of staff)
   */
  fun calculateLinePosition(midiNote: Int): Int {
    // Each semitone is half a line
    // Middle C (60) is positioned at line 10 (the middle line)
    // Going up one semitone moves up half a line (line index decreases)
    // Going down one semitone moves down half a line (line index increases)
    val halfLinesFromMiddleC = MIDDLE_C - midiNote
    return MIDDLE_C_LINE + halfLinesFromMiddleC / 2
  }

  /**
   * Determines if the given MIDI note needs a sharp, flat, or natural symbol.
   * 
   * @param midiNote MIDI note number
   * @return Accidental type or null if no accidental needed
   */
  fun getAccidental(midiNote: Int): Accidental? {
    // Determine if the note is a black key
    val pitchClass = midiNote % 12
    
    // Return accidental based on the predefined map
    return NOTE_TO_ACCIDENTAL[pitchClass]
  }

  /**
   * Determines if the given staff position requires a ledger line.
   * 
   * @param linePosition The line position (0-indexed from top of staff)
   * @return True if a ledger line is needed
   */
  fun needsLedgerLine(linePosition: Int): Boolean {
    // Ledger lines are needed for notes above or below the staff
    // Treble staff is lines 0-4, Bass staff is lines 12-16
    // Also need ledger lines for middle C (10) and the space below it (11)
    return linePosition < 0 || // Above treble staff
           (linePosition > 4 && linePosition < 12 && linePosition % 2 == 0) || // Between staffs (only on lines)
           linePosition > 16 // Below bass staff
  }

  /**
   * Returns information about which staff (treble or bass) a note belongs to.
   * 
   * @param linePosition The staff line position
   * @return The staff the note belongs to
   */
  fun determineStaff(linePosition: Int): Staff {
    return when {
      linePosition <= 6 -> Staff.TREBLE
      linePosition >= 10 -> Staff.BASS
      else -> Staff.MIDDLE // Notes between staffs could go either way
    }
  }
  
  /**
   * Converts a MIDI note number to a note name (e.g., "C4", "F#5")
   * 
   * @param midiNote The MIDI note number
   * @return The note name with octave
   */
  fun getNoteNameWithAccidental(midiNote: Int): String {
    val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octave = (midiNote / 12) - 1
    val noteName = noteNames[midiNote % 12]
    return "$noteName$octave"
  }

  /**
   * Enum for different types of accidentals.
   */
  enum class Accidental {
    SHARP, FLAT, NATURAL
  }
  
  /**
   * Enum to identify which staff a note is placed on.
   */
  enum class Staff {
    TREBLE, BASS, MIDDLE
  }
} 
package com.midichords.model

/**
 * Interface for identifying chords from a collection of notes.
 */
interface ChordIdentifier {
  /**
   * Identify a chord from a collection of active notes.
   * @param notes The collection of active notes
   * @return The identified chord, or null if no chord could be identified
   */
  fun identifyChord(notes: List<ActiveNote>): Chord?
  
  /**
   * Identify a chord from a collection of pitch classes.
   * @param pitchClasses The collection of pitch classes
   * @return The identified chord, or null if no chord could be identified
   */
  fun identifyChord(pitchClasses: List<PitchClass>): Chord?
  
  /**
   * Register a listener to be notified when a chord is identified.
   * @param listener The listener to register
   */
  fun registerChordListener(listener: ChordListener)
  
  /**
   * Unregister a previously registered chord listener.
   * @param listener The listener to unregister
   */
  fun unregisterChordListener(listener: ChordListener)
} 
package com.midichords.model

/**
 * Interface for listening to chord identification events.
 */
interface ChordListener {
  /**
   * Called when a chord is identified.
   * @param chord The identified chord
   * @param notes The notes that were used to identify the chord
   */
  fun onChordIdentified(chord: Chord, notes: List<ActiveNote>)
  
  /**
   * Called when no chord could be identified from the current notes.
   * @param notes The notes that were analyzed
   */
  fun onNoChordIdentified(notes: List<ActiveNote>)
} 
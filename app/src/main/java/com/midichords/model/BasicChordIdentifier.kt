package com.midichords.model

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Basic implementation of the ChordIdentifier interface that identifies
 * common chords from a collection of notes.
 */
class BasicChordIdentifier : ChordIdentifier, NoteStateListener {
  companion object {
    private const val TAG = "BasicChordIdentifier"
    private const val MIN_NOTES_FOR_CHORD = 3
  }

  private val listeners = CopyOnWriteArrayList<ChordListener>()
  private var lastIdentifiedChord: Chord? = null
  private var currentNotes = listOf<ActiveNote>()

  override fun onNoteActivated(note: ActiveNote) {
    // Not used directly, we rely on onActiveNotesChanged
  }

  override fun onNoteDeactivated(note: ActiveNote) {
    // Not used directly, we rely on onActiveNotesChanged
  }

  override fun onActiveNotesChanged(activeNotes: List<ActiveNote>) {
    currentNotes = activeNotes
    val chord = identifyChordFromNotes(activeNotes)
    
    if (chord != null) {
      if (chord != lastIdentifiedChord) {
        lastIdentifiedChord = chord
        notifyChordIdentified(chord, activeNotes)
      }
    } else if (lastIdentifiedChord != null) {
      lastIdentifiedChord = null
      notifyNoChordIdentified(activeNotes)
    }
  }

  override fun onSustainPedalStateChanged(isOn: Boolean) {
    // Not used for chord identification
  }

  @JvmName("identifyChordFromNotes")
  override fun identifyChord(notes: List<ActiveNote>): Chord? {
    if (notes.size < MIN_NOTES_FOR_CHORD) {
      return null
    }

    // Extract pitch classes from notes
    val pitchClasses = notes.map { Note.fromMidiNote(it.noteNumber).pitchClass }.distinct()
    return identifyChord(pitchClasses)
  }

  @JvmName("identifyChordFromPitchClasses")
  override fun identifyChord(pitchClasses: List<PitchClass>): Chord? {
    if (pitchClasses.size < MIN_NOTES_FOR_CHORD) {
      return null
    }

    // Try all possible roots and find the best match
    for (potentialRoot in pitchClasses) {
      val chord = identifyChordWithRoot(pitchClasses, potentialRoot)
      if (chord != null) {
        return chord
      }
    }

    // Try to identify with bass note as root (for inversions)
    val bassNote = findBassNote()
    if (bassNote != null) {
      val bassPC = Note.fromMidiNote(bassNote.noteNumber).pitchClass
      if (!pitchClasses.contains(bassPC)) {
        // Bass note is not in the chord, might be a slash chord
        val chordWithoutBass = identifyChord(pitchClasses)
        if (chordWithoutBass != null) {
          return Chord(
            chordWithoutBass.root,
            chordWithoutBass.type,
            chordWithoutBass.inversion,
            bassPC
          )
        }
      } else {
        // Try with bass note as root first
        val chord = identifyChordWithRoot(pitchClasses, bassPC)
        if (chord != null) {
          return chord
        }
      }
    }

    return null
  }

  private fun identifyChordWithRoot(pitchClasses: List<PitchClass>, root: PitchClass): Chord? {
    // Calculate intervals from the root
    val intervals = pitchClasses.map { pc ->
      (pc.position - root.position + 12) % 12
    }.sorted().distinct()

    // Ensure root is included
    if (!intervals.contains(0)) {
      return null
    }

    // Try to match with known chord types
    val chordType = ChordType.findByIntervals(intervals) ?: return null

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

    return Chord(root, chordType, inversion, bassPC)
  }

  private fun findBassNote(): ActiveNote? {
    if (currentNotes.isEmpty()) {
      return null
    }
    
    // Find the note with the lowest MIDI note number
    return currentNotes.minByOrNull { it.noteNumber }
  }

  override fun registerChordListener(listener: ChordListener) {
    listeners.add(listener)
  }

  override fun unregisterChordListener(listener: ChordListener) {
    listeners.remove(listener)
  }

  private fun notifyChordIdentified(chord: Chord, notes: List<ActiveNote>) {
    Log.d(TAG, "Chord identified: ${chord.getName()}")
    listeners.forEach { it.onChordIdentified(chord, notes) }
  }

  private fun notifyNoChordIdentified(notes: List<ActiveNote>) {
    Log.d(TAG, "No chord identified from ${notes.size} notes")
    listeners.forEach { it.onNoChordIdentified(notes) }
  }
  
  // Helper method to avoid recursive calls after adding @JvmName annotations
  private fun identifyChordFromNotes(notes: List<ActiveNote>): Chord? {
    if (notes.size < MIN_NOTES_FOR_CHORD) {
      return null
    }

    // Extract pitch classes from notes
    val pitchClasses = notes.map { Note.fromMidiNote(it.noteNumber).pitchClass }.distinct()
    return identifyChordFromPitchClasses(pitchClasses)
  }
  
  private fun identifyChordFromPitchClasses(pitchClasses: List<PitchClass>): Chord? {
    return identifyChord(pitchClasses)
  }
} 
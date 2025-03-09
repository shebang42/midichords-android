package com.midichords.model

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class BasicChordIdentifierTest {

  private lateinit var chordIdentifier: BasicChordIdentifier
  
  @Mock
  private lateinit var chordListener: ChordListener

  @Before
  fun setup() {
    chordIdentifier = BasicChordIdentifier()
    chordIdentifier.registerChordListener(chordListener)
  }

  // Helper function to create an ActiveNote for testing
  private fun createActiveNote(midiNoteNumber: Int, velocity: Int = 100): ActiveNote {
    return ActiveNote(
      noteNumber = midiNoteNumber,
      velocity = velocity,
      timestamp = System.currentTimeMillis(),
      channel = 0,
      isSustained = false
    )
  }

  @Test
  fun `test minimum notes required for chord`() {
    // Less than 3 notes should not identify a chord
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64)  // E4
    )
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener, never()).onChordIdentified(any(), any())
    verify(chordListener).onNoChordIdentified(notes)
  }

  @Test
  fun `test major chord identification`() {
    // C Major (C, E, G)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67)  // G4
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.MAJOR)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
    verify(chordListener, never()).onNoChordIdentified(any())
  }

  @Test
  fun `test minor chord identification`() {
    // A Minor (A, C, E)
    val notes = listOf(
      createActiveNote(69), // A4
      createActiveNote(72), // C5
      createActiveNote(76)  // E5
    )
    
    val expectedChord = Chord(PitchClass.A, ChordType.MINOR)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test first inversion major chord`() {
    // C Major, first inversion (E, G, C)
    val notes = listOf(
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(72)  // C5
    )
    
    // The chord should still be identified as C Major, but with inversion 1
    val expectedChord = Chord(
      root = PitchClass.C,
      type = ChordType.MAJOR,
      inversion = 1,
      bassNote = PitchClass.E
    )
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test second inversion major chord`() {
    // C Major, second inversion (G, C, E)
    val notes = listOf(
      createActiveNote(67), // G4
      createActiveNote(72), // C5
      createActiveNote(76)  // E5
    )
    
    // The chord should still be identified as C Major, but with inversion 2
    val expectedChord = Chord(
      root = PitchClass.C,
      type = ChordType.MAJOR,
      inversion = 2,
      bassNote = PitchClass.G
    )
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test dominant 7th chord identification`() {
    // G7 (G, B, D, F)
    val notes = listOf(
      createActiveNote(67), // G4
      createActiveNote(71), // B4
      createActiveNote(74), // D5
      createActiveNote(77)  // F5
    )
    
    val expectedChord = Chord(PitchClass.G, ChordType.DOMINANT_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test major 7th chord identification`() {
    // Cmaj7 (C, E, G, B)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(71)  // B4
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.MAJOR_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test minor 7th chord identification`() {
    // Dm7 (D, F, A, C)
    val notes = listOf(
      createActiveNote(62), // D4
      createActiveNote(65), // F4
      createActiveNote(69), // A4
      createActiveNote(72)  // C5
    )
    
    val expectedChord = Chord(PitchClass.D, ChordType.MINOR_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test diminished chord identification`() {
    // B diminished (B, D, F)
    val notes = listOf(
      createActiveNote(71), // B4
      createActiveNote(74), // D5
      createActiveNote(77)  // F5
    )
    
    val expectedChord = Chord(PitchClass.B, ChordType.DIMINISHED)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test augmented chord identification`() {
    // C augmented (C, E, G#)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(68)  // G#4
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.AUGMENTED)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test suspended chords`() {
    // Csus4 (C, F, G)
    val notesSus4 = listOf(
      createActiveNote(60), // C4
      createActiveNote(65), // F4
      createActiveNote(67)  // G4
    )
    
    val expectedChordSus4 = Chord(PitchClass.C, ChordType.SUSPENDED_4)
    
    chordIdentifier.onActiveNotesChanged(notesSus4)
    verify(chordListener).onChordIdentified(expectedChordSus4, notesSus4)
    
    // Csus2 (C, D, G)
    val notesSus2 = listOf(
      createActiveNote(60), // C4
      createActiveNote(62), // D4
      createActiveNote(67)  // G4
    )
    
    val expectedChordSus2 = Chord(PitchClass.C, ChordType.SUSPENDED_2)
    
    chordIdentifier.onActiveNotesChanged(notesSus2)
    verify(chordListener).onChordIdentified(expectedChordSus2, notesSus2)
  }

  @Test
  fun `test slash chord identification`() {
    // C/G (C major with G in the bass)
    val notes = listOf(
      createActiveNote(67), // G4 (bass)
      createActiveNote(72), // C5
      createActiveNote(76)  // E5
    )
    
    val expectedChord = Chord(
      root = PitchClass.C,
      type = ChordType.MAJOR,
      inversion = 2, // Second inversion (G is the bass)
      bassNote = PitchClass.G
    )
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test chord with duplicate notes`() {
    // C major with doubled C
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(72)  // C5 (duplicate of C4)
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.MAJOR)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test chord identification with sustain pedal`() {
    // Start with C Major
    val cMajorNotes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67)  // G4
    )
    
    chordIdentifier.onActiveNotesChanged(cMajorNotes)
    
    // Now activate sustain pedal - not directly modifying notes but setting their isSustained flag
    val sustainedNotes = cMajorNotes.map { it.copy(isSustained = true) }
    chordIdentifier.onActiveNotesChanged(sustainedNotes)
    
    verify(chordListener).onChordIdentified(Chord(PitchClass.C, ChordType.MAJOR), sustainedNotes)
  }
} 
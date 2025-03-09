package com.midichords.model

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class AdvancedChordIdentifierTest {

  private lateinit var chordIdentifier: AdvancedChordIdentifier
  
  @Mock
  private lateinit var chordListener: ChordListener

  @Before
  fun setup() {
    chordIdentifier = AdvancedChordIdentifier()
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
  fun `test partial chord detection`() {
    // C Major 7 without the 5th (C, E, B)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(71)  // B4
    )
    
    // Should still be identified as Cmaj7 even without the 5th (G)
    val expectedChord = Chord(PitchClass.C, ChordType.MAJOR_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test incomplete extended chord detection`() {
    // C9 with missing 7th (C, E, G, D5)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(74)  // D5 (9th)
    )
    
    // Should be identified as C9 despite missing the 7th
    val expectedChord = Chord(PitchClass.C, ChordType.DOMINANT_9TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test added note detection`() {
    // C Major with added 9th (C, E, G, D5)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(74)  // D5 (added 9th)
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.ADDED_9TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test altered chord detection`() {
    // G7b5 (G, B, Db, F)
    val notes = listOf(
      createActiveNote(67), // G4
      createActiveNote(71), // B4
      createActiveNote(73), // Db5 (flat 5)
      createActiveNote(77)  // F5 (minor 7th)
    )
    
    val expectedChord = Chord(PitchClass.G, ChordType.SEVENTH_FLAT_5)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test sixth chord detection`() {
    // C6 (C, E, G, A)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(69)  // A4 (6th)
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.SIXTH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test extended chord with alterations`() {
    // G13b9 (G, B, D, F, Ab, E)
    val notes = listOf(
      createActiveNote(67), // G4
      createActiveNote(71), // B4
      createActiveNote(74), // D5
      createActiveNote(77), // F5 (7th)
      createActiveNote(80), // Ab5 (flat 9th)
      createActiveNote(88)  // E6 (13th)
    )
    
    val expectedChord = Chord(PitchClass.G, ChordType.SEVENTH_FLAT_9)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test minor-major 7th chord`() {
    // Cm(maj7) (C, Eb, G, B)
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(63), // Eb4
      createActiveNote(67), // G4
      createActiveNote(71)  // B4 (major 7th)
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.MINOR_MAJOR_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test half-diminished chord`() {
    // Bø (B, D, F, A)
    val notes = listOf(
      createActiveNote(71), // B4
      createActiveNote(74), // D5
      createActiveNote(77), // F5
      createActiveNote(81)  // A5
    )
    
    val expectedChord = Chord(PitchClass.B, ChordType.HALF_DIMINISHED_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test diminished 7th chord`() {
    // B° (B, D, F, Ab)
    val notes = listOf(
      createActiveNote(71), // B4
      createActiveNote(74), // D5
      createActiveNote(77), // F5
      createActiveNote(80)  // Ab5
    )
    
    val expectedChord = Chord(PitchClass.B, ChordType.DIMINISHED_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }

  @Test
  fun `test chord with added notes and missing critical notes`() {
    // Complicated voicing: C major with missing 3rd but added 9th and 11th
    // (C, G, D, F) - could be identified as C11 or Gsus4/C
    val notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(67), // G4
      createActiveNote(74), // D5 (9th)
      createActiveNote(77)  // F5 (11th)
    )
    
    // The advanced identifier should be able to make a sensible guess
    // In this case, C11 is a reasonable interpretation
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(any(), any())
    // We can't assert the exact chord as it might depend on the implementation
    // but we want to make sure some chord was identified
  }

  @Test
  fun `test unusual voicing detection`() {
    // Cmaj7 with widely spaced notes (C2, B3, E4, G5)
    val notes = listOf(
      createActiveNote(36), // C2
      createActiveNote(71), // B3
      createActiveNote(64), // E4
      createActiveNote(79)  // G5
    )
    
    val expectedChord = Chord(PitchClass.C, ChordType.MAJOR_7TH)
    
    chordIdentifier.onActiveNotesChanged(notes)
    verify(chordListener).onChordIdentified(expectedChord, notes)
  }
  
  // Additional tests for voice leading and complex chord progressions
  
  @Test
  fun `test voice leading with closely related chords`() {
    // Play a ii-V-I progression with voice leading
    
    // ii: Dm7 (D, F, A, C)
    val dm7Notes = listOf(
      createActiveNote(62), // D4
      createActiveNote(65), // F4
      createActiveNote(69), // A4
      createActiveNote(72)  // C5
    )
    
    val expectedDm7 = Chord(PitchClass.D, ChordType.MINOR_7TH)
    chordIdentifier.onActiveNotesChanged(dm7Notes)
    verify(chordListener).onChordIdentified(expectedDm7, dm7Notes)
    
    // V: G7 (G, B, D, F) - keeping some common tones from Dm7
    val g7Notes = listOf(
      createActiveNote(62), // D4 (common tone from Dm7)
      createActiveNote(67), // G4
      createActiveNote(71), // B4
      createActiveNote(65)  // F4 (common tone from Dm7)
    )
    
    val expectedG7 = Chord(PitchClass.G, ChordType.DOMINANT_7TH)
    chordIdentifier.onActiveNotesChanged(g7Notes)
    verify(chordListener).onChordIdentified(expectedG7, g7Notes)
    
    // I: CMaj7 (C, E, G, B) - keeping some common tones from G7
    val cmaj7Notes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67), // G4 (common tone from G7)
      createActiveNote(71)  // B4 (common tone from G7)
    )
    
    val expectedCmaj7 = Chord(PitchClass.C, ChordType.MAJOR_7TH)
    chordIdentifier.onActiveNotesChanged(cmaj7Notes)
    verify(chordListener).onChordIdentified(expectedCmaj7, cmaj7Notes)
  }
  
  @Test
  fun `test poly-chord detection`() {
    // Poly-chord: G triad over C triad (C, E, G, B, D)
    val polyChordNotes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(71), // B4
      createActiveNote(74)  // D5
    )
    
    // This should detect as Cmaj9
    val expectedChord = Chord(PitchClass.C, ChordType.MAJOR_9TH)
    
    chordIdentifier.onActiveNotesChanged(polyChordNotes)
    verify(chordListener).onChordIdentified(expectedChord, polyChordNotes)
  }
  
  @Test
  fun `test chord with ambiguous interpretation`() {
    // The notes E, G, B, D can be interpreted as either Em7 or G6
    val ambiguousNotes = listOf(
      createActiveNote(64), // E4
      createActiveNote(67), // G4
      createActiveNote(71), // B4
      createActiveNote(74)  // D5
    )
    
    // The identification could go either way, but the bass note should help decide
    // With E as the lowest note, Em7 is the more likely interpretation
    chordIdentifier.onActiveNotesChanged(ambiguousNotes)
    verify(chordListener).onChordIdentified(any(), any())
    
    // Now try with G as the lowest note
    val ambiguousNotesWithGBass = listOf(
      createActiveNote(55), // G3
      createActiveNote(64), // E4
      createActiveNote(71), // B4
      createActiveNote(74)  // D5
    )
    
    // With G as the lowest note, G6 is the more likely interpretation
    val expectedG6 = Chord(PitchClass.G, ChordType.SIXTH)
    chordIdentifier.onActiveNotesChanged(ambiguousNotesWithGBass)
    verify(chordListener).onChordIdentified(expectedG6, ambiguousNotesWithGBass)
  }
  
  @Test
  fun `test chord detection with enharmonic equivalents`() {
    // Db major (Db, F, Ab)
    val dbMajorNotes = listOf(
      createActiveNote(61), // Db4
      createActiveNote(65), // F4
      createActiveNote(68)  // Ab4
    )
    
    val expectedDbMajor = Chord(PitchClass.C_SHARP, ChordType.MAJOR)
    chordIdentifier.onActiveNotesChanged(dbMajorNotes)
    verify(chordListener).onChordIdentified(expectedDbMajor, dbMajorNotes)
    
    // C# major (C#, E#, G#) - enharmonic equivalent to Db major
    // Note: E# is F, G# is Ab in our MIDI notation
    val cSharpMajorNotes = listOf(
      createActiveNote(61), // C#4
      createActiveNote(65), // E#4 (F4)
      createActiveNote(68)  // G#4 (Ab4)
    )
    
    // The identification should be C# major (same notes, different spelling)
    val expectedCSharpMajor = Chord(PitchClass.C_SHARP, ChordType.MAJOR)
    chordIdentifier.onActiveNotesChanged(cSharpMajorNotes)
    verify(chordListener).onChordIdentified(expectedCSharpMajor, cSharpMajorNotes)
  }
  
  @Test
  fun `test chord detection with close voicings`() {
    // C major with all notes within one octave, packed closely together
    val closeVoicingNotes = listOf(
      createActiveNote(60), // C4
      createActiveNote(64), // E4
      createActiveNote(67)  // G4
    )
    
    val expectedCMajor = Chord(PitchClass.C, ChordType.MAJOR)
    chordIdentifier.onActiveNotesChanged(closeVoicingNotes)
    verify(chordListener).onChordIdentified(expectedCMajor, closeVoicingNotes)
    
    // C major with a cluster voicing (C, D#, E, G) - including a chromatic non-chord tone
    val clusterVoicingNotes = listOf(
      createActiveNote(60), // C4
      createActiveNote(63), // D#4 (non-chord tone)
      createActiveNote(64), // E4
      createActiveNote(67)  // G4
    )
    
    // The identification should still be C major despite the non-chord tone
    chordIdentifier.onActiveNotesChanged(clusterVoicingNotes)
    verify(chordListener).onChordIdentified(expectedCMajor, clusterVoicingNotes)
  }
} 
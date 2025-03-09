package com.midichords.model

import org.junit.Test
import org.junit.Assert.*

class ChordTest {

  @Test
  fun `test chord name formatting`() {
    // Test major chord name formatting
    val cMajor = Chord(PitchClass.C, ChordType.MAJOR)
    assertEquals("C", cMajor.getName())
    assertEquals("C Major", cMajor.getFullName())
    
    // Test minor chord name formatting
    val aMinor = Chord(PitchClass.A, ChordType.MINOR)
    assertEquals("Am", aMinor.getName())
    assertEquals("A Minor", aMinor.getFullName())
    
    // Test 7th chord name formatting
    val g7 = Chord(PitchClass.G, ChordType.DOMINANT_7TH)
    assertEquals("G7", g7.getName())
    assertEquals("G Dominant 7th", g7.getFullName())
    
    // Test chord with inversion
    val cMajorFirstInv = Chord(PitchClass.C, ChordType.MAJOR, 1, PitchClass.E)
    assertEquals("C/E", cMajorFirstInv.getName())
    assertEquals("C Major/E (1st inversion)", cMajorFirstInv.getFullName())
    
    // Test complex chord
    val dmin9 = Chord(PitchClass.D, ChordType.MINOR_9TH)
    assertEquals("Dm9", dmin9.getName())
    assertEquals("D Minor 9th", dmin9.getFullName())
    
    // Test altered chord
    val a7flat5 = Chord(PitchClass.A, ChordType.SEVENTH_FLAT_5)
    assertEquals("A7b5", a7flat5.getName())
    assertEquals("A Seventh Flat 5", a7flat5.getFullName())
  }

  @Test
  fun `test flat notation vs sharp notation`() {
    // Test sharp notation
    val fSharpMajor = Chord(PitchClass.F_SHARP, ChordType.MAJOR)
    assertEquals("F#", fSharpMajor.getName(useFlats = false))
    
    // Test flat notation
    assertEquals("Gb", fSharpMajor.getName(useFlats = true))
    
    // Test Db vs C#
    val cSharpMinor = Chord(PitchClass.C_SHARP, ChordType.MINOR)
    assertEquals("C#m", cSharpMinor.getName(useFlats = false))
    assertEquals("Dbm", cSharpMinor.getName(useFlats = true))
  }

  @Test
  fun `test chord contains pitch class`() {
    // C major contains C, E, G
    val cMajor = Chord(PitchClass.C, ChordType.MAJOR)
    assertTrue(cMajor.contains(PitchClass.C))
    assertTrue(cMajor.contains(PitchClass.E))
    assertTrue(cMajor.contains(PitchClass.G))
    assertFalse(cMajor.contains(PitchClass.D))
    assertFalse(cMajor.contains(PitchClass.F))
    
    // G7 contains G, B, D, F
    val g7 = Chord(PitchClass.G, ChordType.DOMINANT_7TH)
    assertTrue(g7.contains(PitchClass.G))
    assertTrue(g7.contains(PitchClass.B))
    assertTrue(g7.contains(PitchClass.D))
    assertTrue(g7.contains(PitchClass.F))
    assertFalse(g7.contains(PitchClass.A))
  }

  @Test
  fun `test get pitch classes in chord`() {
    // C major contains C, E, G
    val cMajor = Chord(PitchClass.C, ChordType.MAJOR)
    val cMajorPitchClasses = cMajor.getPitchClasses()
    assertEquals(3, cMajorPitchClasses.size)
    assertTrue(cMajorPitchClasses.contains(PitchClass.C))
    assertTrue(cMajorPitchClasses.contains(PitchClass.E))
    assertTrue(cMajorPitchClasses.contains(PitchClass.G))
    
    // G7 contains G, B, D, F
    val g7 = Chord(PitchClass.G, ChordType.DOMINANT_7TH)
    val g7PitchClasses = g7.getPitchClasses()
    assertEquals(4, g7PitchClasses.size)
    assertTrue(g7PitchClasses.contains(PitchClass.G))
    assertTrue(g7PitchClasses.contains(PitchClass.B))
    assertTrue(g7PitchClasses.contains(PitchClass.D))
    assertTrue(g7PitchClasses.contains(PitchClass.F))
  }

  @Test
  fun `test chord factory methods`() {
    // Test major chord factory
    val cMajor = Chord.major(PitchClass.C)
    assertEquals(ChordType.MAJOR, cMajor.type)
    assertEquals(PitchClass.C, cMajor.root)
    
    // Test minor chord factory
    val aMinor = Chord.minor(PitchClass.A)
    assertEquals(ChordType.MINOR, aMinor.type)
    assertEquals(PitchClass.A, aMinor.root)
    
    // Test dominant7 chord factory
    val g7 = Chord.dominant7(PitchClass.G)
    assertEquals(ChordType.DOMINANT_7TH, g7.type)
    assertEquals(PitchClass.G, g7.root)
    
    // Test major7 chord factory
    val cMaj7 = Chord.major7(PitchClass.C)
    assertEquals(ChordType.MAJOR_7TH, cMaj7.type)
    assertEquals(PitchClass.C, cMaj7.root)
    
    // Test minor7 chord factory
    val am7 = Chord.minor7(PitchClass.A)
    assertEquals(ChordType.MINOR_7TH, am7.type)
    assertEquals(PitchClass.A, am7.root)
    
    // Test altered chord factory
    val g7alt = Chord.altered(PitchClass.G)
    assertEquals(ChordType.ALTERED, g7alt.type)
    assertEquals(PitchClass.G, g7alt.root)
    
    // Test slash chord factory
    val c_e = Chord.slashChord(Chord.major(PitchClass.C), PitchClass.E)
    assertEquals(ChordType.MAJOR, c_e.type)
    assertEquals(PitchClass.C, c_e.root)
    assertEquals(PitchClass.E, c_e.bassNote)
    assertEquals(1, c_e.inversion)
  }

  @Test
  fun `test withInversion method`() {
    // Test creating inversions
    val cMajor = Chord(PitchClass.C, ChordType.MAJOR)
    
    // First inversion
    val firstInv = cMajor.withInversion(1)
    assertEquals(1, firstInv.inversion)
    assertEquals(PitchClass.E, firstInv.bassNote)
    
    // Second inversion
    val secondInv = cMajor.withInversion(2)
    assertEquals(2, secondInv.inversion)
    assertEquals(PitchClass.G, secondInv.bassNote)
    
    // Test invalid inversion
    assertThrows(IllegalArgumentException::class.java) {
      cMajor.withInversion(3) // C Major only has 3 notes, so inversion 3 is invalid
    }
    
    // Test seventh chord inversions
    val g7 = Chord(PitchClass.G, ChordType.DOMINANT_7TH)
    
    // Third inversion of seventh chord
    val thirdInv = g7.withInversion(3)
    assertEquals(3, thirdInv.inversion)
    assertEquals(PitchClass.F, thirdInv.bassNote)
  }
  
  @Test
  fun `test withBassNote method`() {
    // Test creating slash chords with withBassNote method
    val cMajor = Chord(PitchClass.C, ChordType.MAJOR)
    
    // C/E (first inversion) - bass note is part of the chord
    val cWithE = cMajor.withBassNote(PitchClass.E)
    assertEquals(1, cWithE.inversion)
    assertEquals(PitchClass.E, cWithE.bassNote)
    assertEquals("C/E", cWithE.getName())
    
    // C/G (second inversion) - bass note is part of the chord
    val cWithG = cMajor.withBassNote(PitchClass.G)
    assertEquals(2, cWithG.inversion)
    assertEquals(PitchClass.G, cWithG.bassNote)
    assertEquals("C/G", cWithG.getName())
    
    // C/B (non-standard slash chord) - bass note is not part of the chord
    val cWithB = cMajor.withBassNote(PitchClass.B)
    assertEquals(0, cWithB.inversion) // Inversion remains 0
    assertEquals(PitchClass.B, cWithB.bassNote)
    assertEquals("C/B", cWithB.getName())
    
    // Test bass note same as root
    val cWithC = cMajor.withBassNote(PitchClass.C)
    assertEquals(0, cWithC.inversion)
    assertEquals(PitchClass.C, cWithC.bassNote)
    assertEquals("C", cWithC.getName()) // No slash notation when bass = root
  }
  
  @Test
  fun `test alternative chord names`() {
    // Test C/E alternative interpretation as Em7/C
    val cWithE = Chord(PitchClass.C, ChordType.MAJOR, 1, PitchClass.E)
    val altName = cWithE.getAlternativeName()
    assertNotNull(altName)
    assertEquals("Em7/C", altName)
    
    // Test C/G alternative interpretation as Gsus4(add7)
    val cWithG = Chord(PitchClass.C, ChordType.MAJOR, 2, PitchClass.G)
    val altName2 = cWithG.getAlternativeName()
    assertNotNull(altName2)
    assertEquals("Gsus4(add7)", altName2)
    
    // Test Cmaj7 alternative interpretation as Em/C
    val cMaj7 = Chord(PitchClass.C, ChordType.MAJOR_7TH)
    val altName3 = cMaj7.getAlternativeName()
    assertNotNull(altName3)
    assertEquals("Am/C", altName3)
    
    // Test standard C Major - should have no alternative
    val cMajor = Chord(PitchClass.C, ChordType.MAJOR)
    val altName4 = cMajor.getAlternativeName()
    assertNull(altName4)
    
    // Test half-diminished chord
    val bHalfDim = Chord(PitchClass.B, ChordType.HALF_DIMINISHED_7TH)
    val altName5 = bHalfDim.getAlternativeName()
    assertNotNull(altName5)
    assertEquals("G#maj7/B", altName5)
    assertEquals("Abmaj7/B", bHalfDim.getAlternativeName(true)) // Using flats
  }
  
  @Test
  fun `test jazz chord names`() {
    // Test altered dominant
    val g7alt = Chord(PitchClass.G, ChordType.ALTERED)
    assertEquals("G7alt", g7alt.getJazzName())
    
    // Test Lydian dominant
    val c7sharp11 = Chord(PitchClass.C, ChordType.LYDIAN_DOMINANT)
    assertEquals("C7#11", c7sharp11.getJazzName())
    
    // Test half-diminished (should use ø symbol)
    val dHalfDim = Chord(PitchClass.D, ChordType.HALF_DIMINISHED_7TH)
    assertEquals("Dø", dHalfDim.getJazzName())
    
    // Test quartal chord
    val cQuartal = Chord(PitchClass.C, ChordType.QUARTAL)
    assertEquals("C4ths", cQuartal.getJazzName())
    
    // Test flat-9 dominant 13th
    val eflat13flat9 = Chord(PitchClass.D_SHARP, ChordType.DOMINANT_13_FLAT_9)
    assertEquals("D#13♭9", eflat13flat9.getJazzName()) // Using sharps
    assertEquals("Eb13♭9", eflat13flat9.getJazzName(true)) // Using flats
    
    // Test slash chord with jazz notation
    val g7altWithC = Chord(PitchClass.G, ChordType.ALTERED, 0, PitchClass.C)
    assertEquals("G7alt/C", g7altWithC.getJazzName())
  }
  
  @Test
  fun `test chord voicing`() {
    // Test root position voicing
    val cMajor = Chord(PitchClass.C, ChordType.MAJOR)
    val cVoicing = cMajor.getVoicing()
    assertEquals(3, cVoicing.size)
    assertEquals(PitchClass.C, cVoicing[0])
    assertEquals(PitchClass.E, cVoicing[1])
    assertEquals(PitchClass.G, cVoicing[2])
    
    // Test first inversion voicing (E-G-C)
    val cMajorFirst = cMajor.withInversion(1)
    val cFirstVoicing = cMajorFirst.getVoicing()
    assertEquals(3, cFirstVoicing.size)
    assertEquals(PitchClass.E, cFirstVoicing[0])
    assertEquals(PitchClass.G, cFirstVoicing[1])
    assertEquals(PitchClass.C, cFirstVoicing[2])
    
    // Test second inversion voicing (G-C-E)
    val cMajorSecond = cMajor.withInversion(2)
    val cSecondVoicing = cMajorSecond.getVoicing()
    assertEquals(3, cSecondVoicing.size)
    assertEquals(PitchClass.G, cSecondVoicing[0])
    assertEquals(PitchClass.C, cSecondVoicing[1])
    assertEquals(PitchClass.E, cSecondVoicing[2])
    
    // Test dominant 7th voicing
    val g7 = Chord(PitchClass.G, ChordType.DOMINANT_7TH)
    val g7Voicing = g7.getVoicing()
    assertEquals(4, g7Voicing.size)
    assertEquals(PitchClass.G, g7Voicing[0])
    assertEquals(PitchClass.B, g7Voicing[1])
    assertEquals(PitchClass.D, g7Voicing[2])
    assertEquals(PitchClass.F, g7Voicing[3])
    
    // Test third inversion of dominant 7th (F-G-B-D)
    val g7Third = g7.withInversion(3)
    val g7ThirdVoicing = g7Third.getVoicing()
    assertEquals(4, g7ThirdVoicing.size)
    assertEquals(PitchClass.F, g7ThirdVoicing[0])
    assertEquals(PitchClass.G, g7ThirdVoicing[1])
    assertEquals(PitchClass.B, g7ThirdVoicing[2])
    assertEquals(PitchClass.D, g7ThirdVoicing[3])
  }
  
  @Test
  fun `test extended jazz chord formations`() {
    // Test So What chord (Miles Davis modal voicing)
    val dSoWhat = Chord(PitchClass.D, ChordType.SO_WHAT)
    assertEquals("Dso what", dSoWhat.getJazzName())
    assertEquals(5, dSoWhat.getPitchClasses().size)
    
    // Test Quartal harmony
    val cQuartal = Chord(PitchClass.C, ChordType.QUARTAL)
    assertEquals("C4ths", cQuartal.getJazzName())
    assertEquals(4, cQuartal.getPitchClasses().size)
    
    // Test complex altered chord
    val g13b9sharp11 = Chord(PitchClass.G, ChordType.DOMINANT_13_FLAT_9_SHARP_11)
    assertEquals("G13♭9#11", g13b9sharp11.getJazzName())
    val pitchClasses = g13b9sharp11.getPitchClasses()
    assertTrue(pitchClasses.contains(PitchClass.G))  // Root
    assertTrue(pitchClasses.contains(PitchClass.B))  // Major 3rd
    assertTrue(pitchClasses.contains(PitchClass.D))  // Perfect 5th
    assertTrue(pitchClasses.contains(PitchClass.F))  // Dominant 7th
    assertTrue(pitchClasses.contains(PitchClass.G_SHARP)) // Flat 9th (Ab)
    assertTrue(pitchClasses.contains(PitchClass.C_SHARP)) // Sharp 11th (C#)
    assertTrue(pitchClasses.contains(PitchClass.E))  // 13th
  }
  
  @Test
  fun `test slash chord factory and inversions`() {
    // Create D/F# chord (first inversion)
    val dWithFSharp = Chord.slashChord(Chord.major(PitchClass.D), PitchClass.F_SHARP)
    assertEquals("D/F#", dWithFSharp.getName())
    assertEquals(PitchClass.D, dWithFSharp.root)
    assertEquals(PitchClass.F_SHARP, dWithFSharp.bassNote)
    assertEquals(1, dWithFSharp.inversion)
    
    // Create Cm7/Eb chord (first inversion)
    val cm7WithEb = Chord.slashChord(Chord.minor7(PitchClass.C), PitchClass.D_SHARP) // Eb = D#
    assertEquals("Cm7/Eb", cm7WithEb.getName(useFlats = true))
    assertEquals(PitchClass.C, cm7WithEb.root)
    assertEquals(PitchClass.D_SHARP, cm7WithEb.bassNote)
    assertEquals(1, cm7WithEb.inversion)
    
    // Create G7/D chord (second inversion)
    val g7WithD = Chord.slashChord(Chord.dominant7(PitchClass.G), PitchClass.D)
    assertEquals("G7/D", g7WithD.getName())
    assertEquals(PitchClass.G, g7WithD.root)
    assertEquals(PitchClass.D, g7WithD.bassNote)
    assertEquals(2, g7WithD.inversion)
    
    // Create C/A chord (non-standard slash chord - A is not in C major)
    val cWithA = Chord.slashChord(Chord.major(PitchClass.C), PitchClass.A)
    assertEquals("C/A", cWithA.getName())
    assertEquals(PitchClass.C, cWithA.root)
    assertEquals(PitchClass.A, cWithA.bassNote)
    assertEquals(0, cWithA.inversion) // Inversion is 0 since A is not in C major chord
  }
} 
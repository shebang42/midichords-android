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
} 
package com.midichords.model

import org.junit.Test
import org.junit.Assert.*

class ChordTypeTest {

  @Test
  fun `test chord type intervals`() {
    // Test major chord intervals
    val majorIntervals = ChordType.MAJOR.intervals
    assertEquals(listOf(0, 4, 7), majorIntervals)
    
    // Test minor chord intervals
    val minorIntervals = ChordType.MINOR.intervals
    assertEquals(listOf(0, 3, 7), minorIntervals)
    
    // Test 7th chord intervals
    val dom7Intervals = ChordType.DOMINANT_7TH.intervals
    assertEquals(listOf(0, 4, 7, 10), dom7Intervals)
    
    val maj7Intervals = ChordType.MAJOR_7TH.intervals
    assertEquals(listOf(0, 4, 7, 11), maj7Intervals)
    
    val min7Intervals = ChordType.MINOR_7TH.intervals
    assertEquals(listOf(0, 3, 7, 10), min7Intervals)
    
    // Test extended chord intervals
    val dom9Intervals = ChordType.DOMINANT_9TH.intervals
    assertEquals(listOf(0, 4, 7, 10, 14), dom9Intervals)
    
    val dom13Intervals = ChordType.DOMINANT_13TH.intervals
    assertEquals(listOf(0, 4, 7, 10, 14, 17, 21), dom13Intervals)
  }
  
  @Test
  fun `test findByIntervals with exact matches`() {
    // Find major chord by intervals
    val majorChord = ChordType.findByIntervals(listOf(0, 4, 7))
    assertEquals(ChordType.MAJOR, majorChord)
    
    // Find minor chord by intervals
    val minorChord = ChordType.findByIntervals(listOf(0, 3, 7))
    assertEquals(ChordType.MINOR, minorChord)
    
    // Find dominant 7th chord by intervals
    val dom7Chord = ChordType.findByIntervals(listOf(0, 4, 7, 10))
    assertEquals(ChordType.DOMINANT_7TH, dom7Chord)
    
    // Find augmented chord by intervals
    val augChord = ChordType.findByIntervals(listOf(0, 4, 8))
    assertEquals(ChordType.AUGMENTED, augChord)
    
    // Find diminished chord by intervals
    val dimChord = ChordType.findByIntervals(listOf(0, 3, 6))
    assertEquals(ChordType.DIMINISHED, dimChord)
  }
  
  @Test
  fun `test findByIntervals with unsorted intervals`() {
    // Intervals not in order
    val majorChordUnsorted = ChordType.findByIntervals(listOf(4, 0, 7))
    assertEquals(ChordType.MAJOR, majorChordUnsorted)
    
    // Complex chord with intervals not in order
    val dom9ChordUnsorted = ChordType.findByIntervals(listOf(10, 14, 4, 0, 7))
    assertEquals(ChordType.DOMINANT_9TH, dom9ChordUnsorted)
  }
  
  @Test
  fun `test findByIntervals with intervals not starting from 0`() {
    // Intervals representing 1st inversion of major chord (E, G, C)
    val firstInversionMajor = ChordType.findByIntervals(listOf(4, 7, 12))
    assertEquals(ChordType.MAJOR, firstInversionMajor)
    
    // Intervals representing 2nd inversion of minor chord (G, C, Eb)
    val secondInversionMinor = ChordType.findByIntervals(listOf(7, 12, 15))
    assertEquals(ChordType.MINOR, secondInversionMinor)
  }
  
  @Test
  fun `test findByIntervals with non-existent chord types`() {
    // Random intervals that don't represent a known chord type
    val nonExistentChord = ChordType.findByIntervals(listOf(0, 1, 6))
    assertNull(nonExistentChord)
    
    // Almost a major chord but with wrong intervals
    val almostMajorChord = ChordType.findByIntervals(listOf(0, 5, 7))
    assertNull(almostMajorChord)
  }
  
  @Test
  fun `test chord symbols`() {
    // Test major chord symbol (empty string)
    assertEquals("", ChordType.MAJOR.symbol)
    
    // Test minor chord symbol
    assertEquals("m", ChordType.MINOR.symbol)
    
    // Test dominant 7th chord symbol
    assertEquals("7", ChordType.DOMINANT_7TH.symbol)
    
    // Test major 7th chord symbol
    assertEquals("maj7", ChordType.MAJOR_7TH.symbol)
    
    // Test suspended chords
    assertEquals("sus2", ChordType.SUSPENDED_2.symbol)
    assertEquals("sus4", ChordType.SUSPENDED_4.symbol)
    
    // Test altered chords
    assertEquals("7b5", ChordType.SEVENTH_FLAT_5.symbol)
    assertEquals("7#5", ChordType.SEVENTH_SHARP_5.symbol)
  }
  
  @Test
  fun `test chord full names`() {
    // Test basic chord names
    assertEquals("Major", ChordType.MAJOR.fullName)
    assertEquals("Minor", ChordType.MINOR.fullName)
    
    // Test seventh chord names
    assertEquals("Dominant 7th", ChordType.DOMINANT_7TH.fullName)
    assertEquals("Major 7th", ChordType.MAJOR_7TH.fullName)
    assertEquals("Minor 7th", ChordType.MINOR_7TH.fullName)
    
    // Test extended chord names
    assertEquals("Dominant 9th", ChordType.DOMINANT_9TH.fullName)
    assertEquals("Major 11th", ChordType.MAJOR_11TH.fullName)
    assertEquals("Minor 13th", ChordType.MINOR_13TH.fullName)
    
    // Test altered chord names
    assertEquals("Seventh Flat 5", ChordType.SEVENTH_FLAT_5.fullName)
    assertEquals("Seventh Sharp 5 Flat 9", ChordType.SEVENTH_SHARP_5_FLAT_9.fullName)
  }
  
  @Test
  fun `test jazz chord intervals`() {
    // Test altered dominant intervals
    val alteredDomChord = ChordType.ALTERED.intervals
    assertEquals(listOf(0, 4, 8, 10, 13, 15), alteredDomChord)
    
    // Test Lydian dominant intervals
    val lydianDomChord = ChordType.LYDIAN_DOMINANT.intervals 
    assertEquals(listOf(0, 4, 7, 10, 14, 18, 21), lydianDomChord)
    
    // Test suspended dominant intervals
    val sus4Dom7Chord = ChordType.SUSPENDED_4_7.intervals
    assertEquals(listOf(0, 5, 7, 10), sus4Dom7Chord)
    
    // Test quartal chord intervals
    val quartalChord = ChordType.QUARTAL.intervals
    assertEquals(listOf(0, 5, 10, 15), quartalChord)
    
    // Test So What chord intervals (modal jazz voicing from Kind of Blue)
    val soWhatChord = ChordType.SO_WHAT.intervals
    assertEquals(listOf(0, 5, 10, 15, 19), soWhatChord)
  }
  
  @Test
  fun `test jazz chord symbol representations`() {
    // Test altered dominant symbol
    assertEquals("7alt", ChordType.ALTERED.symbol)
    
    // Test Lydian dominant symbol
    assertEquals("7#11", ChordType.LYDIAN_DOMINANT.symbol)
    
    // Test complex altered dominant symbols
    assertEquals("13b9", ChordType.DOMINANT_13_FLAT_9.symbol)
    assertEquals("13#9", ChordType.DOMINANT_13_SHARP_9.symbol)
    assertEquals("13b9#11", ChordType.DOMINANT_13_FLAT_9_SHARP_11.symbol)
    
    // Test polychord symbols
    assertEquals("maj/7", ChordType.TRIAD_SLASH_SEVENTH.symbol)
    assertEquals("m/7", ChordType.MINOR_TRIAD_SLASH_SEVENTH.symbol)
  }
  
  @Test
  fun `test jazz chord identification with findByIntervals`() {
    // Find altered dominant by intervals
    val alteredChord = ChordType.findByIntervals(listOf(0, 4, 8, 10, 13, 15))
    assertEquals(ChordType.ALTERED, alteredChord)
    
    // Find Lydian dominant by intervals (G7#11: G-B-D-F-A-C#)
    val lydianDomChord = ChordType.findByIntervals(listOf(0, 4, 7, 10, 14, 18, 21))
    assertEquals(ChordType.LYDIAN_DOMINANT, lydianDomChord)
    
    // Find sus4(7) chord by intervals 
    val sus4Dom7 = ChordType.findByIntervals(listOf(0, 5, 7, 10))
    assertEquals(ChordType.SUSPENDED_4_7, sus4Dom7)
    
    // Find quartal chord by intervals
    val quartal = ChordType.findByIntervals(listOf(0, 5, 10, 15))
    assertEquals(ChordType.QUARTAL, quartal)
    
    // Find complex altered dominant chord by intervals
    val dom13b9 = ChordType.findByIntervals(listOf(0, 4, 7, 10, 13, 17, 21))
    assertEquals(ChordType.DOMINANT_13_FLAT_9, dom13b9)
  }
  
  @Test
  fun `test jazz chord inversions and alternative voicings`() {
    // Test quartal chord in different voicing
    val quartalAlt = ChordType.findByIntervals(listOf(5, 10, 15, 20))
    assertEquals(ChordType.QUARTAL, quartalAlt)
    
    // Test 1st inversion of sus4(7) chord
    val sus4Dom7Inv = ChordType.findByIntervals(listOf(5, 7, 10, 12))
    assertEquals(ChordType.SUSPENDED_4_7, sus4Dom7Inv)
    
    // Test minor with extensions chord - using intervals that correspond with a known chord type
    val minorExt = ChordType.findByIntervals(listOf(3, 7, 10, 14, 17))
    assertEquals(ChordType.MINOR_11TH, minorExt)
  }
} 
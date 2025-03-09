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
} 
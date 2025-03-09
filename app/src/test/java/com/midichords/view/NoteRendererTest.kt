package com.midichords.view

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for the NoteRenderer class
 */
class NoteRendererTest {

  private lateinit var noteRenderer: NoteRenderer
  
  @Before
  fun setup() {
    noteRenderer = NoteRenderer()
  }

  @Test
  fun `test calculateLinePosition for middle C`() {
    // Given middle C (MIDI note 60)
    val midiNote = 60
    
    // When calculating the staff position
    val position = noteRenderer.calculateLinePosition(midiNote)
    
    // Then we expect it to be on the middle C line (10)
    assertEquals(10, position)
  }

  @Test
  fun `test calculateLinePosition for notes above middle C`() {
    // Test E above middle C (MIDI note 64)
    assertEquals(8, noteRenderer.calculateLinePosition(64))
    
    // Test G above middle C (MIDI note 67)
    assertEquals(6, noteRenderer.calculateLinePosition(67))
    
    // Test C above middle C (MIDI note 72)
    assertEquals(4, noteRenderer.calculateLinePosition(72))
    
    // Test high C (MIDI note 84)
    assertEquals(-2, noteRenderer.calculateLinePosition(84))
  }

  @Test
  fun `test calculateLinePosition for notes below middle C`() {
    // Test A below middle C (MIDI note 57)
    assertEquals(11, noteRenderer.calculateLinePosition(57))
    
    // Test F below middle C (MIDI note 53)
    assertEquals(13, noteRenderer.calculateLinePosition(53))
    
    // Test C below middle C (MIDI note 48)
    assertEquals(16, noteRenderer.calculateLinePosition(48))
    
    // Test low C (MIDI note 36)
    assertEquals(22, noteRenderer.calculateLinePosition(36))
  }

  @Test
  fun `test getAccidental for natural notes`() {
    // Test C (MIDI note 60)
    assertNull(noteRenderer.getAccidental(60))
    
    // Test E (MIDI note 64)
    assertNull(noteRenderer.getAccidental(64))
    
    // Test G (MIDI note 67)
    assertNull(noteRenderer.getAccidental(67))
  }

  @Test
  fun `test getAccidental for sharp notes`() {
    // Test C# (MIDI note 61)
    assertEquals(NoteRenderer.Accidental.SHARP, noteRenderer.getAccidental(61))
    
    // Test F# (MIDI note 66)
    assertEquals(NoteRenderer.Accidental.SHARP, noteRenderer.getAccidental(66))
    
    // Test G# (MIDI note 68)
    assertEquals(NoteRenderer.Accidental.SHARP, noteRenderer.getAccidental(68))
  }

  @Test
  fun `test needsLedgerLine for notes within staff`() {
    // Notes on the treble staff (from top to bottom) should not need ledger lines
    assertFalse(noteRenderer.needsLedgerLine(0)) // Top line of treble staff
    assertFalse(noteRenderer.needsLedgerLine(1)) // Space below top line
    assertFalse(noteRenderer.needsLedgerLine(2)) // Second line from top
    assertFalse(noteRenderer.needsLedgerLine(3)) // Space below second line
    assertFalse(noteRenderer.needsLedgerLine(4)) // Bottom line of treble staff
    
    // Notes on the bass staff (from top to bottom) should not need ledger lines
    assertFalse(noteRenderer.needsLedgerLine(12)) // Top line of bass staff
    assertFalse(noteRenderer.needsLedgerLine(13)) // Space below top line
    assertFalse(noteRenderer.needsLedgerLine(14)) // Second line from top
    assertFalse(noteRenderer.needsLedgerLine(15)) // Space below second line
    assertFalse(noteRenderer.needsLedgerLine(16)) // Bottom line of bass staff
  }

  @Test
  fun `test needsLedgerLine for notes outside staff`() {
    // Notes above treble staff should need ledger lines
    assertTrue(noteRenderer.needsLedgerLine(-2)) // C above treble staff
    
    // Notes between staffs should need ledger lines
    assertTrue(noteRenderer.needsLedgerLine(6)) // Note between staffs
    assertTrue(noteRenderer.needsLedgerLine(8)) // Note between staffs
    assertTrue(noteRenderer.needsLedgerLine(10)) // Middle C
    
    // Notes below bass staff should need ledger lines
    assertTrue(noteRenderer.needsLedgerLine(18)) // Note below bass staff
    assertTrue(noteRenderer.needsLedgerLine(20)) // Note below bass staff
  }
} 
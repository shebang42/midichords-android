package com.midichords.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import com.midichords.model.ActiveNote
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * A test-specific subclass of StaffView that exposes protected methods for testing
 */
class TestStaffView(context: Context) : StaffView(context) {
  // Expose protected methods for testing
  public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
  }
  
  public override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
  }
}

/**
 * Tests for the StaffView class
 */
@RunWith(MockitoJUnitRunner::class)
class StaffViewTest {

  @Mock
  private lateinit var mockContext: Context
  
  @Mock
  private lateinit var mockCanvas: Canvas
  
  private lateinit var staffView: TestStaffView

  @Before
  fun setup() {
    staffView = TestStaffView(mockContext)
    
    // Set the size of the view
    staffView.onSizeChanged(800, 600, 0, 0)
  }

  @Test
  fun `test staff rendering on empty staff`() {
    // When the staff is empty
    staffView.setActiveNotes(emptyList())
    
    // And we render it
    staffView.onDraw(mockCanvas)
    
    // Then we expect the staff lines to be drawn (10 staff lines + 1 middle C line)
    // Verify that drawLine was called at least 11 times (5 for treble + 5 for bass + 1 for middle C)
    verify(mockCanvas, atLeast(11)).drawLine(
      anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(Paint::class.java)
    )
  }

  @Test
  fun `test rendering multiple notes`() {
    // Given multiple active notes
    val notes = listOf(
      ActiveNote(noteNumber = 60, velocity = 100, channel = 0, timestamp = System.currentTimeMillis()), // Middle C
      ActiveNote(noteNumber = 64, velocity = 100, channel = 0, timestamp = System.currentTimeMillis()), // E above middle C
      ActiveNote(noteNumber = 67, velocity = 100, channel = 0, timestamp = System.currentTimeMillis())  // G above middle C
    )
    
    // When we set these notes
    staffView.setActiveNotes(notes)
    
    // And render
    staffView.onDraw(mockCanvas)
    
    // Then we expect note heads to be drawn for each note
    // Each note gets an oval (for the note head)
    verify(mockCanvas, times(3)).drawOval(
      anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(Paint::class.java)
    )
    
    // And note stems
    verify(mockCanvas, times(3)).drawLine(
      anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(Paint::class.java)
    )
    
    // And note names (for debugging)
    verify(mockCanvas, times(3)).drawText(
      anyString(), anyFloat(), anyFloat(), any(Paint::class.java)
    )
  }

  @Test
  fun `test rendering notes with ledger lines`() {
    // Given notes that need ledger lines
    val notes = listOf(
      ActiveNote(noteNumber = 48, velocity = 100, channel = 0, timestamp = System.currentTimeMillis()), // C below middle C
      ActiveNote(noteNumber = 84, velocity = 100, channel = 0, timestamp = System.currentTimeMillis())  // C two octaves above middle C
    )
    
    // When we set these notes
    staffView.setActiveNotes(notes)
    
    // And render
    staffView.onDraw(mockCanvas)
    
    // Then we expect ledger lines to be drawn
    // Verify ledger lines were drawn (at least the note heads + stems + some ledger lines)
    verify(mockCanvas, atLeast(7)).drawLine(
      anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(Paint::class.java)
    )
  }

  @Test
  fun `test rendering with updated notes`() {
    // Given an initial set of notes
    val initialNotes = listOf(
      ActiveNote(noteNumber = 60, velocity = 100, channel = 0, timestamp = System.currentTimeMillis()) // Middle C
    )
    
    // When we set and render
    staffView.setActiveNotes(initialNotes)
    staffView.onDraw(mockCanvas)
    
    // Then update to a new set
    val updatedNotes = listOf(
      ActiveNote(noteNumber = 64, velocity = 100, channel = 0, timestamp = System.currentTimeMillis()), // E above middle C
      ActiveNote(noteNumber = 67, velocity = 100, channel = 0, timestamp = System.currentTimeMillis())  // G above middle C
    )
    
    // When we set these notes
    staffView.setActiveNotes(updatedNotes)
    
    // And render again
    val newCanvas = mock(Canvas::class.java)
    staffView.onDraw(newCanvas)
    
    // Then we expect to see the new notes drawn
    // Verify the new canvas has 2 ovals drawn (note heads)
    verify(newCanvas, times(2)).drawOval(
      anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(Paint::class.java)
    )
  }
} 
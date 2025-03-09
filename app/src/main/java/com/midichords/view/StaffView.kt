package com.midichords.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.midichords.model.ActiveNote
import kotlin.math.min

/**
 * Custom view for rendering a musical staff with notes.
 */
open class StaffView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  companion object {
    private const val TAG = "StaffView"
    
    // Constants for staff rendering based on standard music notation
    private const val STAFF_LINES = 5
    private const val LEDGER_LINE_EXTENSION = 4  // Shorter ledger lines for better look
    private const val MIDDLE_C_LINE = 10 // 0-indexed line number for middle C
    private const val TREBLE_TOP_LINE = 0 // The top line of the treble staff
    private const val BASS_BOTTOM_LINE = 16 // The bottom line of the bass staff
    private const val STAFF_LINE_SPACING = 6 // Reduced for tighter, more standard spacing
    
    // Constants for note spacing and alignment
    private const val CLEF_MARGIN = 50f // Reduced for more balanced layout
    private const val HORIZONTAL_NOTE_POSITION = 0.28f // Adjusted for better alignment
    private const val MIN_VERTICAL_NOTE_SPACING = 1 // Tighter spacing
  }

  // Paint objects for rendering
  private val staffPaint = Paint().apply {
    color = Color.BLACK
    strokeWidth = 1f
    isAntiAlias = true
  }
  
  private val notePaint = Paint().apply {
    color = Color.BLACK
    style = Paint.Style.FILL
    isAntiAlias = true
  }
  
  private val noteOutlinePaint = Paint().apply {
    color = Color.BLACK
    style = Paint.Style.STROKE
    strokeWidth = 1.5f
    isAntiAlias = true
  }
  
  private val textPaint = Paint().apply {
    color = Color.BLACK
    textSize = 48f // Smaller for better proportions
    typeface = Typeface.DEFAULT_BOLD
    isAntiAlias = true
  }

  // Dimensions and metrics
  private var staffWidth = 0
  private var staffHeight = 0
  private var lineSpace = 0f // Calculated space between staff lines
  private var staffY = 0f // Y position of the top staff line
  
  // Notes to display
  private var activeNotes: List<ActiveNote> = emptyList()
  
  // NoteRenderer for handling note drawing
  private val noteRenderer = NoteRenderer()

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    staffWidth = w
    staffHeight = h
    
    // Calculate spacing based on view height
    lineSpace = (staffHeight / 22f).coerceAtLeast(STAFF_LINE_SPACING.toFloat())
    
    // Center the staff vertically
    staffY = (staffHeight - lineSpace * 16) / 2
    
    Log.d(TAG, "Staff dimensions: $staffWidth x $staffHeight, lineSpace: $lineSpace, staffY: $staffY")
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    
    // Draw treble and bass staffs
    drawStaff(canvas)
    
    // Draw clefs
    drawClefs(canvas)
    
    // Draw active notes
    drawNotes(canvas)
  }

  /**
   * Draws the 5-line staff for both treble and bass clefs
   */
  private fun drawStaff(canvas: Canvas) {
    // Draw treble staff (top 5 lines)
    for (i in 0 until STAFF_LINES) {
      val y = staffY + i * lineSpace
      canvas.drawLine(0f, y, staffWidth.toFloat(), y, staffPaint)
    }
    
    // Draw bass staff (bottom 5 lines)
    for (i in 0 until STAFF_LINES) {
      val lineIndex = BASS_BOTTOM_LINE - i
      val y = staffY + lineIndex * lineSpace
      canvas.drawLine(0f, y, staffWidth.toFloat(), y, staffPaint)
    }
    
    // Draw middle line (middle C position) with dashed line
    val middleCY = staffY + MIDDLE_C_LINE * lineSpace
    staffPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 3f), 0f)
    canvas.drawLine(0f, middleCY, staffWidth.toFloat(), middleCY, staffPaint)
    staffPaint.pathEffect = null
  }

  /**
   * Draws treble and bass clef symbols
   */
  private fun drawClefs(canvas: Canvas) {
    // For simplicity, just draw text representation of clefs
    textPaint.textAlign = Paint.Align.LEFT
    canvas.drawText("𝄞", 8f, staffY + 1.9f * lineSpace, textPaint) // Treble clef (G clef)
    canvas.drawText("𝄢", 8f, staffY + 14.1f * lineSpace, textPaint) // Bass clef (F clef)
  }

  /**
   * Draws all active notes on the staff
   */
  private fun drawNotes(canvas: Canvas) {
    if (activeNotes.isEmpty()) return
    
    // Sort notes by pitch (high to low) for consistent visualization
    val sortedNotes = activeNotes.sortedByDescending { it.noteNumber }
    
    // Calculate the horizontal position for the notes (vertically aligned)
    val noteX = CLEF_MARGIN + (staffWidth - CLEF_MARGIN) * HORIZONTAL_NOTE_POSITION
    
    // Group notes to avoid overlapping accidentals 
    var lastNotePosition = -100 // Arbitrary initial value
    var noteOffsetX = 0f
    
    for (note in sortedNotes) {
      val linePosition = noteRenderer.calculateLinePosition(note.noteNumber)
      
      // If this note is too close to the previous one, offset it horizontally
      val verticalDistance = Math.abs(linePosition - lastNotePosition)
      if (verticalDistance < MIN_VERTICAL_NOTE_SPACING) {
        noteOffsetX += lineSpace * 2f
      } else {
        noteOffsetX = 0f
      }
      
      drawNote(canvas, note, noteX + noteOffsetX)
      lastNotePosition = linePosition
    }
  }

  /**
   * Draws a single note on the staff
   */
  private fun drawNote(canvas: Canvas, note: ActiveNote, x: Float) {
    // Calculate position
    val line = noteRenderer.calculateLinePosition(note.noteNumber)
    val y = staffY + line * lineSpace
    
    // Draw ledger lines if needed
    drawLedgerLines(canvas, line, x)
    
    // Draw accidental if needed (sharp/flat)
    val accidental = noteRenderer.getAccidental(note.noteNumber)
    if (accidental != null) {
      val accidentalString = when (accidental) {
        NoteRenderer.Accidental.SHARP -> "♯"
        NoteRenderer.Accidental.FLAT -> "♭"
        NoteRenderer.Accidental.NATURAL -> "♮"
      }
      
      val accidentalPaint = Paint(textPaint).apply {
        textSize = lineSpace * 1.8f
        textAlign = Paint.Align.CENTER
      }
      
      canvas.drawText(accidentalString, x - lineSpace * 1.5f, y + lineSpace / 3, accidentalPaint)
    }
    
    // Draw the note head as a whole note (oval with hollow center)
    val noteHeadWidth = lineSpace * 1.1f
    val noteHeadHeight = lineSpace * 0.7f
    
    // Draw filled oval then overlay with white/hollow center for whole note
    canvas.drawOval(
      x - noteHeadWidth / 2,
      y - noteHeadHeight / 2, 
      x + noteHeadWidth / 2,
      y + noteHeadHeight / 2,
      notePaint
    )
    
    // Draw hollow center for whole note effect
    val innerWidth = noteHeadWidth * 0.5f
    val innerHeight = noteHeadHeight * 0.4f
    val holeColor = Color.WHITE
    val holePaint = Paint(notePaint).apply { color = holeColor }
    
    canvas.drawOval(
      x - innerWidth / 2,
      y - innerHeight / 2,
      x + innerWidth / 2,
      y + innerHeight / 2,
      holePaint
    )
  }

  /**
   * Draws ledger lines for notes that are outside the staff
   */
  private fun drawLedgerLines(canvas: Canvas, line: Int, x: Float) {
    val noteWidth = lineSpace * 1.2f
    val ledgerLineWidth = noteWidth + LEDGER_LINE_EXTENSION * 2
    
    // Draw ledger lines above treble staff
    if (line < TREBLE_TOP_LINE) {
      for (i in TREBLE_TOP_LINE - 2 downTo line step 2) { 
        if (i % 2 == 0) { // Only draw on line positions (even numbers)
          val y = staffY + i * lineSpace
          canvas.drawLine(x - ledgerLineWidth / 2, y, 
                         x + ledgerLineWidth / 2, y, staffPaint)
        }
      }
    }
    
    // Draw ledger lines between treble and bass staffs
    if (line > 4 && line < 12 && line % 2 == 0) {
      val y = staffY + line * lineSpace
      canvas.drawLine(x - ledgerLineWidth / 2, y, 
                     x + ledgerLineWidth / 2, y, staffPaint)
    }
    
    // Draw ledger lines below bass staff
    if (line > BASS_BOTTOM_LINE) {
      for (i in BASS_BOTTOM_LINE + 2..line step 2) {
        if (i % 2 == 0) { // Only draw on line positions (even numbers)
          val y = staffY + i * lineSpace
          canvas.drawLine(x - ledgerLineWidth / 2, y, 
                         x + ledgerLineWidth / 2, y, staffPaint)
        }
      }
    }
  }

  /**
   * Updates the notes to be displayed
   */
  fun setActiveNotes(notes: List<ActiveNote>) {
    activeNotes = notes
    invalidate() // Request redraw
  }
} 
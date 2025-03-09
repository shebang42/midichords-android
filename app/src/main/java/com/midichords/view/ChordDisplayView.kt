package com.midichords.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.midichords.model.Chord

/**
 * Custom view for displaying the currently identified chord.
 */
class ChordDisplayView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  // Paint objects for rendering
  private val backgroundPaint = Paint().apply {
    color = Color.parseColor("#E6F5F9")
    style = Paint.Style.FILL
    isAntiAlias = true
  }
  
  private val borderPaint = Paint().apply {
    color = Color.parseColor("#AACCDD")
    style = Paint.Style.STROKE
    strokeWidth = 2f
    isAntiAlias = true
  }
  
  private val textPaint = Paint().apply {
    color = Color.BLACK
    textSize = 64f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
  }
  
  private val detailPaint = Paint().apply {
    color = Color.DKGRAY
    textSize = 48f // Doubled from 24f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
  }
  
  // Background rectangle for chord display
  private val backgroundRect = RectF()
  
  // Current chord to display
  private var currentChord: Chord? = null
  private var noChordMessage = "No Chord"

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    
    // Update background rectangle based on new dimensions
    val padding = 10f
    backgroundRect.set(padding, padding, w - padding, h - padding)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    
    // Draw background and border
    canvas.drawRoundRect(backgroundRect, 15f, 15f, backgroundPaint)
    canvas.drawRoundRect(backgroundRect, 15f, 15f, borderPaint)
    
    // Draw chord name or "No Chord" message
    val centerX = width / 2f
    val centerY = height / 2f - 10f  // Adjust for visual centering with subtexts
    
    if (currentChord != null) {
      // Draw the chord name
      textPaint.textSize = 128f
      canvas.drawText(currentChord!!.getName(), centerX, centerY, textPaint)
      
      // Draw additional info like full name and inversion
      val detailsY = centerY + 80f
      
      // Create detail text combining full name and inversion if applicable
      val fullName = currentChord!!.getFullName()
      val inversion = when(currentChord!!.inversion) {
        0 -> "Root Position"
        1 -> "First Inversion"
        2 -> "Second Inversion"
        3 -> "Third Inversion"
        else -> "Inversion ${currentChord!!.inversion}"
      }
      
      val detailText = "$fullName - $inversion"
      canvas.drawText(detailText, centerX, detailsY, detailPaint)
    } else {
      // Draw the "No Chord" message
      textPaint.textSize = 128f
      canvas.drawText(noChordMessage, centerX, centerY, textPaint)
    }
  }

  /**
   * Updates the chord to be displayed
   */
  fun setChord(chord: Chord?) {
    currentChord = chord
    invalidate() // Request redraw
  }
  
  /**
   * Sets the message to display when no chord is detected
   */
  fun setNoChordMessage(message: String) {
    noChordMessage = message
    invalidate() // Request redraw if no chord is currently displayed
  }
} 
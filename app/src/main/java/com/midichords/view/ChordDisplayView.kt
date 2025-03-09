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
    color = Color.parseColor("#333333")
    textSize = 48f
    typeface = Typeface.DEFAULT_BOLD
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
  }
  
  private val subtextPaint = Paint().apply {
    color = Color.parseColor("#666666")
    textSize = 24f
    typeface = Typeface.DEFAULT
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
  }
  
  // Background rectangle for chord display
  private val backgroundRect = RectF()
  
  // Current chord to display
  private var currentChord: Chord? = null
  private var noChordMessage = "No Chord Detected"

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
      textPaint.textSize = 64f
      canvas.drawText(currentChord!!.getName(), centerX, centerY, textPaint)
      
      // Draw additional info like full name and inversion
      val detailsY = centerY + 40f
      
      // Create detail text combining full name and inversion if applicable
      val inversionText = when (currentChord!!.inversion) {
        0 -> ""
        1 -> "(1st inversion)"
        2 -> "(2nd inversion)"
        3 -> "(3rd inversion)"
        else -> "(${currentChord!!.inversion}th inversion)"
      }
      
      val detailText = if (inversionText.isNotEmpty()) {
        "${currentChord!!.getFullName()} $inversionText"
      } else {
        currentChord!!.getFullName()
      }
      
      canvas.drawText(detailText, centerX, detailsY, subtextPaint)
    } else {
      // Draw the "No Chord" message
      textPaint.textSize = 48f
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
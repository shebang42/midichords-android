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
  
  private val altNamePaint = Paint().apply {
    color = Color.parseColor("#555555")
    textSize = 96f  // Increased from 32f to 96f (3x larger)
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
  }
  
  // Background rectangle for chord display
  private val backgroundRect = RectF()
  
  // Current chord to display
  private var currentChord: Chord? = null
  private var noChordMessage = "No Chord"
  
  // Flag to control whether to show alternative names
  private var showAlternatives = true
  
  // List of alternative names that should be excluded (too weird/uncommon)
  private val excludedPatterns = listOf(
    "dim7(no5)",    // Overly specific
    ".*\\(4ths\\)", // Quartal descriptions are too technical
    ".*\\(add.*"    // Add descriptions are often too complex
  )

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
    
    if (currentChord != null) {
      // Use a fixed position for the main chord regardless of alternatives
      val mainChordY = height * 0.25f
      
      // Draw the main chord name
      textPaint.textSize = 128f
      canvas.drawText(currentChord!!.getName(), centerX, mainChordY, textPaint)
      
      // Draw inversion details
      val inversionText = when(currentChord!!.inversion) {
        0 -> ""
        1 -> "1st inv."
        2 -> "2nd inv."
        3 -> "3rd inv."
        else -> "${currentChord!!.inversion}th inv."
      }
      
      if (inversionText.isNotEmpty()) {
        val inversionY = mainChordY + 100f  // Increased from 70f to account for larger text
        detailPaint.textSize = 80f  // Increased from 32f to 80f (2.5x larger)
        canvas.drawText(inversionText, centerX, inversionY, detailPaint)
      }
      
      // Draw alternative names if enabled
      val hasAlternatives = getFilteredAlternativeNames().isNotEmpty() && showAlternatives
      if (hasAlternatives) {
        val altNames = getFilteredAlternativeNames()
        val startY = mainChordY + 200f  // Increased from 150f to account for larger inversion text
        
        // Display jazz name if available
        val jazzName = getJazzName()
        if (jazzName != null && !altNames.contains(jazzName)) {
          altNames.add(0, jazzName)
        }
        
        // Limit to 3 alternative names
        val displayNames = altNames.take(3)
        
        // Display "Also known as:" text with larger text
        altNamePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        altNamePaint.textSize = 64f  // Slightly smaller than alternative names
        canvas.drawText("Also known as:", centerX, startY, altNamePaint)
        
        // Reset to original size for alternative names
        altNamePaint.textSize = 96f
        altNamePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        displayNames.forEachIndexed { index, name ->
          val y = startY + 110f + (index * 110f)
          canvas.drawText(name, centerX, y, altNamePaint)
        }
      }
    } else {
      // Draw the "No Chord" message
      textPaint.textSize = 128f
      canvas.drawText(noChordMessage, centerX, height * 0.25f, textPaint)
    }
  }

  /**
   * Get filtered list of alternative names that aren't too weird
   */
  private fun getFilteredAlternativeNames(): MutableList<String> {
    val altNames = mutableListOf<String>()
    val chord = currentChord ?: return altNames
    
    // Add the primary alternative name
    val altName = chord.getAlternativeName()
    if (altName != null && isReasonableAltName(altName)) {
      altNames.add(altName)
    }
    
    // Add flat notation version if using sharps, or vice versa
    val currentUsingFlats = chord.getName().contains('b')
    val altWithDifferentNotation = if (currentUsingFlats) {
      chord.getName(useFlats = false)
    } else {
      chord.getName(useFlats = true)
    }
    
    // Only add if it's different from the main name
    if (altWithDifferentNotation != chord.getName() && isReasonableAltName(altWithDifferentNotation)) {
      altNames.add(altWithDifferentNotation)
    }
    
    return altNames
  }
  
  /**
   * Get jazz-specific name if it's different from the standard name
   */
  private fun getJazzName(): String? {
    val chord = currentChord ?: return null
    val jazzName = chord.getJazzName()
    
    // Only return if different from standard name and reasonable
    return if (jazzName != chord.getName() && isReasonableAltName(jazzName)) {
      jazzName
    } else {
      null
    }
  }
  
  /**
   * Check if an alternative name is reasonable (not too weird/complex)
   */
  private fun isReasonableAltName(name: String): Boolean {
    // Check against excluded patterns
    for (pattern in excludedPatterns) {
      if (name.matches(Regex(pattern))) {
        return false
      }
    }
    
    // Additional filtering criteria
    if (name.length > 12) {
      // Too long names are likely too complex
      return false
    }
    
    return true
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
  
  /**
   * Enable or disable showing alternative names
   */
  fun setShowAlternatives(show: Boolean) {
    showAlternatives = show
    invalidate()
  }
  
  /**
   * Check if alternative names are currently being shown
   */
  fun isShowingAlternatives(): Boolean {
    return showAlternatives
  }
} 
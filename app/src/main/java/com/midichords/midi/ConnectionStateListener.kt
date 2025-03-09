package com.midichords.midi

import com.midichords.viewmodel.ConnectionState

/**
 * Interface for listening to MIDI device connection state changes.
 */
interface ConnectionStateListener {
  /**
   * Called when the connection state changes.
   * @param state The new connection state
   * @param message Optional message providing additional details (e.g., error description)
   */
  fun onConnectionStateChanged(state: ConnectionState, message: String? = null)
} 
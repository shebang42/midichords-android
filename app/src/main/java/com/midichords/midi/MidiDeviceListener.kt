package com.midichords.midi

/**
 * Interface for receiving MIDI device connection state changes.
 */
interface MidiDeviceListener {
  /**
   * Called when the connection state of a MIDI device changes.
   * @param state The new connection state
   * @param message A message describing the state change
   */
  fun onConnectionStateChanged(state: ConnectionState, message: String)
} 
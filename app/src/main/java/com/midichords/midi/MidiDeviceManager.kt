package com.midichords.midi

/**
 * Interface for managing MIDI device connections.
 */
interface MidiDeviceManager {
  /**
   * Register a listener for connection state changes.
   * @param listener The listener to register
   */
  fun registerListener(listener: MidiDeviceListener)

  /**
   * Unregister a previously registered listener.
   * @param listener The listener to unregister
   */
  fun unregisterListener(listener: MidiDeviceListener)

  /**
   * Refresh the list of available MIDI devices and attempt to connect to one if available.
   */
  fun refreshAvailableDevices()

  /**
   * Disconnect from the currently connected MIDI device.
   */
  fun disconnect()

  /**
   * Add a listener for MIDI events.
   * @param listener The listener to add
   */
  fun addMidiEventListener(listener: MidiEventListener)

  /**
   * Remove a previously added MIDI event listener.
   * @param listener The listener to remove
   */
  fun removeMidiEventListener(listener: MidiEventListener)
}
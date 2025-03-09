package com.midichords.midi

import android.hardware.usb.UsbDevice

/**
 * Interface for managing MIDI device connections and event dispatching.
 */
interface MidiDeviceManager {
  /**
   * Register a listener to be notified of connection state changes.
   * @param listener The listener to register
   */
  fun registerListener(listener: MidiDeviceListener)

  /**
   * Unregister a previously registered listener.
   * @param listener The listener to unregister
   */
  fun unregisterListener(listener: MidiDeviceListener)

  /**
   * Refresh the list of available MIDI devices.
   * This will scan for USB devices and MIDI devices.
   */
  fun refreshAvailableDevices()

  /**
   * Connect to a specific USB device.
   * This will request permission if needed and attempt to establish a connection.
   * @param device The USB device to connect to
   */
  fun connectToUsbDevice(device: UsbDevice)

  /**
   * Disconnect from the currently connected device.
   * This will release all resources associated with the connection.
   */
  fun disconnect()

  /**
   * Add a listener to be notified of MIDI events.
   * @param listener The listener to add
   */
  fun addMidiEventListener(listener: MidiEventListener)

  /**
   * Remove a previously registered MIDI event listener.
   * @param listener The listener to remove
   */
  fun removeMidiEventListener(listener: MidiEventListener)
  
  /**
   * Clean up all resources associated with this manager.
   * This should be called when the application is being destroyed.
   */
  fun cleanup()
}
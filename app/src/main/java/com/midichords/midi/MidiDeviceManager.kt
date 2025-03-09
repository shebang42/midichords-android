package com.midichords.midi

import android.hardware.usb.UsbDevice

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
   * Register a listener for MIDI events.
   * @param listener The listener to register
   */
  fun registerMidiEventListener(listener: MidiEventListener)

  /**
   * Unregister a previously registered MIDI event listener.
   * @param listener The listener to unregister
   */
  fun unregisterMidiEventListener(listener: MidiEventListener)

  /**
   * Request permission to connect to a USB MIDI device.
   * @param device The USB device to request permission for
   */
  fun requestPermission(device: UsbDevice)

  /**
   * Connect to a USB MIDI device.
   * @param device The USB device to connect to
   * @return true if connection was initiated successfully, false otherwise
   */
  fun connectToDevice(device: UsbDevice): Boolean

  /**
   * Disconnect from the currently connected MIDI device.
   */
  fun disconnect()

  /**
   * Get the list of available USB MIDI devices.
   * @return List of available USB MIDI devices
   */
  fun getAvailableDevices(): List<UsbDevice>

  /**
   * Get the currently connected MIDI device, if any.
   * @return The connected device or null if not connected
   */
  fun getConnectedDevice(): UsbDevice?

  /**
   * Clean up resources when the manager is no longer needed.
   */
  fun dispose()

  fun refreshAvailableDevices()

  fun addMidiEventListener(listener: MidiEventListener)

  fun removeMidiEventListener(listener: MidiEventListener)
} 
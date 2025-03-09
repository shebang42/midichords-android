package com.midichords.midi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

class MidiDeviceManagerImpl(
  private val context: Context,
  private val midiManager: MidiManager?,
  private val usbManager: UsbManager
) : MidiDeviceManager {

  companion object {
    private const val TAG = "MidiDeviceManagerImpl"
    private const val ACTION_USB_PERMISSION = "com.midichords.USB_PERMISSION"
  }

  private val listeners = CopyOnWriteArrayList<MidiDeviceListener>()
  private var currentDevice: MidiDevice? = null
  private val midiInputProcessor = MidiInputProcessorImpl()
  private var currentDeviceInfo: MidiDeviceInfo? = null

  private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACTION_USB_PERMISSION -> {
          synchronized(this) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
              device?.let {
                Log.d(TAG, "USB permission granted for device: ${it.deviceName}")
                connectToUsbDevice(it)
              }
            } else {
              Log.d(TAG, "USB permission denied")
              notifyListeners(ConnectionState.ERROR, "USB permission denied")
            }
          }
        }
        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
          val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          device?.let {
            Log.d(TAG, "USB device attached: ${it.deviceName}")
            requestUsbPermission(it)
          }
        }
        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
          val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          device?.let {
            Log.d(TAG, "USB device detached: ${it.deviceName}")
            disconnect()
          }
        }
      }
    }
  }

  init {
    if (midiManager == null) {
      throw IllegalStateException("MIDI service not available")
    }

    val filter = IntentFilter(ACTION_USB_PERMISSION).apply {
      addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
      addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
    }

    context.registerReceiver(
      usbReceiver,
      filter,
      Context.RECEIVER_NOT_EXPORTED
    )

    Log.d(TAG, "MidiDeviceManager initialized")
  }

  override fun registerListener(listener: MidiDeviceListener) {
    listeners.add(listener)
  }

  override fun unregisterListener(listener: MidiDeviceListener) {
    listeners.remove(listener)
  }

  override fun refreshAvailableDevices() {
    try {
      midiManager?.devices?.let { devices ->
        Log.d(TAG, "Found ${devices.size} MIDI devices")
        if (devices.isEmpty()) {
          notifyListeners(ConnectionState.DISCONNECTED, "No MIDI devices found")
        } else {
          // For now, just try to connect to the first device
          connectToDevice(devices[0])
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing devices", e)
      notifyListeners(ConnectionState.ERROR, "Error refreshing devices: ${e.message}")
    }
  }

  private fun connectToDevice(deviceInfo: MidiDeviceInfo) {
    try {
      midiManager?.openDevice(deviceInfo, { device ->
        if (device != null) {
          currentDevice = device
          currentDeviceInfo = deviceInfo
          setupMidiInput(device)
          notifyListeners(ConnectionState.CONNECTED, "Connected to ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
        } else {
          notifyListeners(ConnectionState.ERROR, "Failed to open MIDI device")
        }
      }, null)
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting to device: ${e.message}")
    }
  }

  private fun setupMidiInput(device: MidiDevice) {
    try {
      val inputPort = device.openInputPort(0)
      if (inputPort != null) {
        inputPort.connect(midiInputProcessor.getReceiver())
        Log.d(TAG, "MIDI input port connected")
      } else {
        Log.e(TAG, "Failed to open MIDI input port")
        notifyListeners(ConnectionState.ERROR, "Failed to open MIDI input port")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error setting up MIDI input", e)
      notifyListeners(ConnectionState.ERROR, "Error setting up MIDI input: ${e.message}")
    }
  }

  private fun connectToUsbDevice(usbDevice: UsbDevice) {
    try {
      midiManager?.devices?.forEach { deviceInfo ->
        if (deviceInfo.type == MidiDeviceInfo.TYPE_USB) {
          val properties = deviceInfo.properties
          val deviceId = properties.getInt(MidiDeviceInfo.PROPERTY_USB_DEVICE)
          if (deviceId == usbDevice.deviceId) {
            connectToDevice(deviceInfo)
            return
          }
        }
      }
      notifyListeners(ConnectionState.ERROR, "USB device not recognized as MIDI device")
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to USB device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting to USB device: ${e.message}")
    }
  }

  private fun requestUsbPermission(device: UsbDevice) {
    val permissionIntent = PendingIntent.getBroadcast(
      context,
      0,
      Intent(ACTION_USB_PERMISSION),
      PendingIntent.FLAG_IMMUTABLE
    )
    usbManager.requestPermission(device, permissionIntent)
  }

  override fun disconnect() {
    try {
      currentDevice?.close()
      currentDevice = null
      currentDeviceInfo = null
      notifyListeners(ConnectionState.DISCONNECTED, "Disconnected")
    } catch (e: Exception) {
      Log.e(TAG, "Error disconnecting", e)
      notifyListeners(ConnectionState.ERROR, "Error disconnecting: ${e.message}")
    }
  }

  private fun notifyListeners(state: ConnectionState, message: String) {
    listeners.forEach { it.onConnectionStateChanged(state, message) }
  }

  override fun addMidiEventListener(listener: MidiEventListener) {
    midiInputProcessor.registerListener(listener)
  }

  override fun removeMidiEventListener(listener: MidiEventListener) {
    midiInputProcessor.unregisterListener(listener)
  }
}
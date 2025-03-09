package com.midichords.midi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.midichords.viewmodel.ConnectionState
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of MidiDeviceManager for handling USB MIDI device connections.
 */
class MidiDeviceManagerImpl(
  private val context: Context,
  private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager,
  private val midiManager: MidiManager? = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
) : MidiDeviceManager {

  init {
    if (midiManager == null) {
      throw IllegalStateException("MIDI service not available on this device")
    }
  }

  companion object {
    private const val TAG = "MidiDeviceManagerImpl"
    private const val ACTION_USB_PERMISSION = "com.midichords.USB_PERMISSION"
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  private val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
  } else {
    PendingIntent.FLAG_UPDATE_CURRENT
  }

  private val listeners = CopyOnWriteArrayList<ConnectionStateListener>()
  private var currentDevice: UsbDevice? = null
  private var midiDevice: MidiDevice? = null
  private var permissionIntent: PendingIntent? = null
  private var deviceCallback: MidiManager.DeviceCallback? = null

  private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      try {
        when (intent.action) {
          ACTION_USB_PERMISSION -> {
            synchronized(this) {
              val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
              } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
              }

              Log.d(TAG, "USB Permission result received for device: ${device?.deviceName}")
              if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                device?.let { connectToDevice(it) }
              } else {
                notifyListeners(ConnectionState.ERROR, "Permission denied for device")
              }
            }
          }
          UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
              @Suppress("DEPRECATION")
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            Log.d(TAG, "USB Device attached: ${device?.deviceName}")
            device?.let { requestPermission(it) }
          }
          UsbManager.ACTION_USB_DEVICE_DETACHED -> {
            Log.d(TAG, "USB Device detached")
            disconnect()
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error in USB receiver", e)
        notifyListeners(ConnectionState.ERROR, "Error processing USB event: ${e.message}")
      }
    }
  }

  init {
    try {
      // Register USB receiver
      Log.d(TAG, "Initializing MidiDeviceManager")
      permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), pendingIntentFlags)
      val filter = IntentFilter().apply {
        addAction(ACTION_USB_PERMISSION)
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
      }
      context.registerReceiver(usbReceiver, filter)
      Log.d(TAG, "USB receiver registered")
      notifyListeners(ConnectionState.DISCONNECTED)
    } catch (e: Exception) {
      Log.e(TAG, "Error initializing MidiDeviceManager", e)
      notifyListeners(ConnectionState.ERROR, "Error initializing MIDI: ${e.message}")
      throw e
    }
  }

  override fun registerListener(listener: ConnectionStateListener) {
    listeners.addIfAbsent(listener)
  }

  override fun unregisterListener(listener: ConnectionStateListener) {
    listeners.remove(listener)
  }

  override fun requestPermission(device: UsbDevice) {
    try {
      Log.d(TAG, "Requesting permission for device: ${device.deviceName}")
      if (usbManager.hasPermission(device)) {
        Log.d(TAG, "Already have permission for device")
        connectToDevice(device)
      } else {
        permissionIntent?.let { usbManager.requestPermission(device, it) }
        notifyListeners(ConnectionState.CONNECTING, "Requesting permission for device")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error requesting permission", e)
      notifyListeners(ConnectionState.ERROR, "Error requesting permission: ${e.message}")
    }
  }

  override fun connectToDevice(device: UsbDevice): Boolean {
    try {
      Log.d(TAG, "Attempting to connect to device: ${device.deviceName}")
      if (!usbManager.hasPermission(device)) {
        Log.d(TAG, "No permission for device, requesting...")
        requestPermission(device)
        return false
      }

      notifyListeners(ConnectionState.CONNECTING)
      
      if (midiManager == null) {
        Log.e(TAG, "MIDI service not available")
        notifyListeners(ConnectionState.ERROR, "MIDI service not available")
        return false
      }

      // Get existing MIDI devices
      val devices = midiManager.devices
      val targetDeviceId = device.deviceId
      
      Log.d(TAG, "Found ${devices.size} MIDI devices")
      
      // Look for an existing device with matching ID
      val existingDevice = devices.firstOrNull { 
        it.properties.getInt(MidiDeviceInfo.PROPERTY_USB_DEVICE, -1) == targetDeviceId
      }

      if (existingDevice != null) {
        Log.d(TAG, "Found existing MIDI device")
        openMidiDevice(existingDevice)
      } else {
        Log.d(TAG, "No existing MIDI device found, creating new connection")
        // Create connection to USB device
        val connection = usbManager.openDevice(device)
        if (connection == null) {
          Log.e(TAG, "Failed to open USB connection")
          notifyListeners(ConnectionState.ERROR, "Failed to open USB connection")
          return false
        }

        // Let the MidiManager handle the device
        deviceCallback = object : MidiManager.DeviceCallback() {
          override fun onDeviceAdded(midiDeviceInfo: MidiDeviceInfo) {
            Log.d(TAG, "MIDI device added: ${midiDeviceInfo.properties}")
            if (midiDeviceInfo.properties.getInt(MidiDeviceInfo.PROPERTY_USB_DEVICE, -1) == targetDeviceId) {
              openMidiDevice(midiDeviceInfo)
              midiManager.unregisterDeviceCallback(this)
              deviceCallback = null
            }
          }
        }.also { callback ->
          midiManager.registerDeviceCallback(callback, mainHandler)
        }
      }

      return true
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting to device: ${e.message}")
      return false
    }
  }

  private fun openMidiDevice(deviceInfo: MidiDeviceInfo) {
    try {
      Log.d(TAG, "Opening MIDI device: ${deviceInfo.properties}")
      midiManager?.openDevice(deviceInfo, { device ->
        if (device != null) {
          Log.d(TAG, "Successfully opened MIDI device")
          midiDevice = device
          currentDevice = getConnectedDevice()
          notifyListeners(ConnectionState.CONNECTED)
        } else {
          Log.e(TAG, "Failed to open MIDI device")
          notifyListeners(ConnectionState.ERROR, "Failed to open MIDI device")
        }
      }, mainHandler)
    } catch (e: Exception) {
      Log.e(TAG, "Error opening MIDI device", e)
      notifyListeners(ConnectionState.ERROR, "Error opening MIDI device: ${e.message}")
    }
  }

  override fun disconnect() {
    try {
      Log.d(TAG, "Disconnecting MIDI device")
      deviceCallback?.let { callback ->
        midiManager?.unregisterDeviceCallback(callback)
        deviceCallback = null
      }
      midiDevice?.close()
      midiDevice = null
      currentDevice = null
      notifyListeners(ConnectionState.DISCONNECTED)
    } catch (e: Exception) {
      Log.e(TAG, "Error disconnecting device", e)
      notifyListeners(ConnectionState.ERROR, "Error disconnecting device: ${e.message}")
    }
  }

  override fun getAvailableDevices(): List<UsbDevice> {
    return try {
      usbManager.deviceList.values.filter { device ->
        device.interfaceCount > 0 && device.getInterface(0).interfaceClass == UsbConstants.USB_CLASS_AUDIO
      }.also { devices ->
        Log.d(TAG, "Found ${devices.size} available USB MIDI devices")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting available devices", e)
      emptyList()
    }
  }

  override fun getConnectedDevice(): UsbDevice? = currentDevice

  override fun dispose() {
    try {
      Log.d(TAG, "Disposing MidiDeviceManager")
      disconnect()
      try {
        context.unregisterReceiver(usbReceiver)
      } catch (e: IllegalArgumentException) {
        // Receiver not registered, ignore
        Log.w(TAG, "USB receiver was not registered")
      }
      listeners.clear()
    } catch (e: Exception) {
      Log.e(TAG, "Error disposing MidiDeviceManager", e)
    }
  }

  private fun notifyListeners(state: ConnectionState, message: String? = null) {
    try {
      mainHandler.post {
        listeners.forEach { listener ->
          try {
            listener.onConnectionStateChanged(state, message)
          } catch (e: Exception) {
            Log.e(TAG, "Error notifying listener", e)
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error in notifyListeners", e)
    }
  }
} 
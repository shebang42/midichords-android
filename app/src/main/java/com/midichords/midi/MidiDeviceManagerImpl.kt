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
import com.midichords.viewmodel.ConnectionState
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of MidiDeviceManager for handling USB MIDI device connections.
 */
class MidiDeviceManagerImpl(
  private val context: Context,
  private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager,
  private val midiManager: MidiManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    context.getSystemService(Context.MIDI_SERVICE) as MidiManager
  } else {
    throw IllegalStateException("MIDI not supported on this Android version")
  }
) : MidiDeviceManager {

  companion object {
    private const val ACTION_USB_PERMISSION = "com.midichords.USB_PERMISSION"
    private const val PENDING_INTENT_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
  }

  private val listeners = CopyOnWriteArrayList<ConnectionStateListener>()
  private var currentDevice: UsbDevice? = null
  private var midiDevice: MidiDevice? = null
  private var permissionIntent: PendingIntent? = null

  private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACTION_USB_PERMISSION -> {
          synchronized(this) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
              @Suppress("DEPRECATION")
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
              device?.let { connect(it) }
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
          device?.let { requestPermission(it) }
        }
        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
          disconnect()
        }
      }
    }
  }

  init {
    // Register USB receiver
    permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), PENDING_INTENT_FLAGS)
    val filter = IntentFilter().apply {
      addAction(ACTION_USB_PERMISSION)
      addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
      addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
    }
    context.registerReceiver(usbReceiver, filter)
  }

  override fun registerListener(listener: ConnectionStateListener) {
    listeners.addIfAbsent(listener)
  }

  override fun unregisterListener(listener: ConnectionStateListener) {
    listeners.remove(listener)
  }

  override fun requestPermission(device: UsbDevice) {
    if (usbManager.hasPermission(device)) {
      connect(device)
    } else {
      permissionIntent?.let { usbManager.requestPermission(device, it) }
      notifyListeners(ConnectionState.CONNECTING, "Requesting permission for device")
    }
  }

  override fun connect(device: UsbDevice): Boolean {
    if (!usbManager.hasPermission(device)) {
      requestPermission(device)
      return false
    }

    notifyListeners(ConnectionState.CONNECTING)
    
    try {
      val deviceInfo = MidiDeviceInfo.Builder()
        .setType(MidiDeviceInfo.TYPE_USB)
        .setId(device.deviceId)
        .setInputPortCount(1)
        .setOutputPortCount(1)
        .setProperties(Bundle().apply {
          putString(MidiDeviceInfo.PROPERTY_NAME, device.deviceName)
          putString(MidiDeviceInfo.PROPERTY_MANUFACTURER, device.manufacturerName)
          putString(MidiDeviceInfo.PROPERTY_PRODUCT, device.productName)
        })
        .build()

      midiManager.openDevice(deviceInfo, { device ->
        if (device != null) {
          midiDevice = device
          currentDevice = this@MidiDeviceManagerImpl.currentDevice
          notifyListeners(ConnectionState.CONNECTED)
        } else {
          notifyListeners(ConnectionState.ERROR, "Failed to open MIDI device")
        }
      }, null)

      return true
    } catch (e: IOException) {
      notifyListeners(ConnectionState.ERROR, "Error connecting to device: ${e.message}")
      return false
    }
  }

  override fun disconnect() {
    midiDevice?.close()
    midiDevice = null
    currentDevice = null
    notifyListeners(ConnectionState.DISCONNECTED)
  }

  override fun getAvailableDevices(): List<UsbDevice> {
    return usbManager.deviceList.values.filter { device ->
      device.interfaceCount > 0 && device.getInterface(0).interfaceClass == UsbConstants.USB_CLASS_AUDIO
    }
  }

  override fun getConnectedDevice(): UsbDevice? = currentDevice

  override fun dispose() {
    disconnect()
    try {
      context.unregisterReceiver(usbReceiver)
    } catch (e: IllegalArgumentException) {
      // Receiver not registered, ignore
    }
    listeners.clear()
  }

  private fun notifyListeners(state: ConnectionState, message: String? = null) {
    listeners.forEach { it.onConnectionStateChanged(state, message) }
  }
} 
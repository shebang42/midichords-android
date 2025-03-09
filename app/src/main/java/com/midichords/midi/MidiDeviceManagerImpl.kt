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
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.media.midi.MidiSender
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    private const val RETRY_INTERVAL_MS = 2000L // 2 seconds between retries
    private const val MAX_RETRIES = 30 // Maximum number of retries (1 minute total with 2-second interval)
  }

  private val listeners = CopyOnWriteArrayList<MidiDeviceListener>()
  private var currentDevice: MidiDevice? = null
  private val midiInputProcessor = MidiInputProcessorImpl()
  private var currentDeviceInfo: MidiDeviceInfo? = null
  private var currentInputPort: MidiInputPort? = null
  
  // Handler and Runnable for retry mechanism
  private val handler = Handler(Looper.getMainLooper())
  private var retryCount = 0
  private var isRetrying = false
  
  private val retryRunnable = Runnable {
    if (retryCount < MAX_RETRIES) {
      Log.d(TAG, "Retrying MIDI device search (attempt ${retryCount + 1}/$MAX_RETRIES)")
      refreshAvailableDevices()
      retryCount++
    } else {
      Log.d(TAG, "Max retries reached, stopping automatic retry")
      stopRetrying()
    }
  }

  private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACTION_USB_PERMISSION -> {
          synchronized(this) {
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
              @Suppress("DEPRECATION")
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            
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
          val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          }
          
          device?.let {
            Log.d(TAG, "USB device attached: ${it.deviceName}")
            // Stop retrying if we were in retry mode
            stopRetrying()
            requestUsbPermission(it)
          }
        }
        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
          val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          }
          
          device?.let {
            Log.d(TAG, "USB device detached: ${it.deviceName}")
            disconnect()
            // Start retrying to find new devices
            startRetrying()
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
      null,
      Handler(context.mainLooper),
      Context.RECEIVER_EXPORTED
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
      // Use the deprecated approach for all Android versions for now
      @Suppress("DEPRECATION")
      val devices = midiManager?.devices ?: emptyArray()
      handleDeviceList(devices)
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing devices", e)
      notifyListeners(ConnectionState.ERROR, "Error refreshing devices: ${e.message}")
    }
  }
  
  private fun handleDeviceList(devices: Array<MidiDeviceInfo>) {
    Log.d(TAG, "Found ${devices.size} MIDI devices")
    if (devices.isEmpty()) {
      notifyListeners(ConnectionState.DISCONNECTED, "No MIDI devices found")
      // Start retry mechanism if not already retrying
      if (!isRetrying) {
        startRetrying()
      }
    } else {
      // Stop retrying since we found devices
      stopRetrying()
      // For now, just try to connect to the first device
      connectToDevice(devices[0])
    }
  }
  
  private fun startRetrying() {
    if (!isRetrying) {
      isRetrying = true
      retryCount = 0
      handler.postDelayed(retryRunnable, RETRY_INTERVAL_MS)
      Log.d(TAG, "Started automatic retry for MIDI device search")
    }
  }
  
  private fun stopRetrying() {
    if (isRetrying) {
      handler.removeCallbacks(retryRunnable)
      isRetrying = false
      retryCount = 0
      Log.d(TAG, "Stopped automatic retry for MIDI device search")
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
          // Start retrying if we couldn't open the device
          startRetrying()
        }
      }, null)
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting to device: ${e.message}")
      // Start retrying if there was an error
      startRetrying()
    }
  }

  private fun setupMidiInput(device: MidiDevice) {
    try {
      val inputPort = device.openInputPort(0)
      if (inputPort != null) {
        // Store the input port
        currentInputPort = inputPort
        
        // Store the input port in the processor
        midiInputProcessor.setInputPort(inputPort)
        
        // For MIDI input ports, we don't need to establish a connection
        // The device will send MIDI data to our app, and we'll process it in the MidiReceiver
        
        Log.d(TAG, "MIDI input port opened successfully")
        notifyListeners(ConnectionState.CONNECTED, "MIDI input port opened successfully")
      } else {
        Log.e(TAG, "Failed to open MIDI input port")
        notifyListeners(ConnectionState.ERROR, "Failed to open MIDI input port")
        // Start retrying if we couldn't open the input port
        startRetrying()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error setting up MIDI input", e)
      notifyListeners(ConnectionState.ERROR, "Error setting up MIDI input: ${e.message}")
      // Start retrying if there was an error
      startRetrying()
    }
  }

  override fun connectToUsbDevice(device: UsbDevice) {
    try {
      // Use the deprecated approach for all Android versions for now
      @Suppress("DEPRECATION")
      val devices = midiManager?.devices ?: emptyArray()
      findAndConnectToUsbDevice(devices, device)
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to USB device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting to USB device: ${e.message}")
      // Start retrying if there was an error
      startRetrying()
    }
  }
  
  private fun findAndConnectToUsbDevice(devices: Array<MidiDeviceInfo>, usbDevice: UsbDevice) {
    for (deviceInfo in devices) {
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
    // Start retrying if we couldn't find the USB device
    startRetrying()
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
      currentInputPort?.close()
      currentInputPort = null
      currentDevice?.close()
      currentDevice = null
      currentDeviceInfo = null
      notifyListeners(ConnectionState.DISCONNECTED, "Disconnected")
      // Start retrying to find new devices
      startRetrying()
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
  
  // Call this method when the app is being destroyed to clean up resources
  override fun cleanup() {
    stopRetrying()
    disconnect()
    try {
      context.unregisterReceiver(usbReceiver)
    } catch (e: Exception) {
      Log.e(TAG, "Error unregistering receiver", e)
    }
  }
}
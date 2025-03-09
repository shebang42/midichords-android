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
import java.util.concurrent.atomic.AtomicBoolean

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
  
  // Variables for direct USB connection
  private var directConnectionThread: Thread? = null
  private var directUsbConnection: android.hardware.usb.UsbDeviceConnection? = null
  private var directUsbInterface: android.hardware.usb.UsbInterface? = null
  
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

  // Device callback for MIDI device detection
  private val deviceCallback = object : MidiManager.DeviceCallback() {
    override fun onDeviceAdded(device: MidiDeviceInfo) {
      Log.d(TAG, "MIDI device added: ${device.properties}")
      handleMidiDevice(device)
    }

    override fun onDeviceRemoved(device: MidiDeviceInfo) {
      Log.d(TAG, "MIDI device removed: ${device.properties}")
      if (device == currentDeviceInfo) {
        disconnect()
      }
    }
  }

  // USB permission receiver
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
            
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (granted) {
              if (device != null) {
                Log.d(TAG, "USB permission granted for device: ${device.deviceName}")
                connectToMidiDevice(device)
              } else {
                Log.e(TAG, "Device is null even though permission was granted")
                notifyListeners(ConnectionState.ERROR, "Device is null")
              }
            } else {
              Log.d(TAG, "USB permission denied for device")
              notifyListeners(ConnectionState.ERROR, "USB permission denied")
            }
          }
        }
        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
          Log.d(TAG, "USB device attached")
          refreshAvailableDevices()
        }
        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
          Log.d(TAG, "USB device detached")
          val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          }
          
          // Check if this is our current device
          if (device != null) {
            Log.d(TAG, "USB device detached: ${device.deviceName}")
            disconnect()
          }
          
          refreshAvailableDevices()
        }
      }
    }
  }

  init {
    if (midiManager == null) {
      throw IllegalStateException("MIDI service not available")
    }

    // Register USB receiver
    val filter = IntentFilter(ACTION_USB_PERMISSION).apply {
      addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
      addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
    }
    
    try {
      context.registerReceiver(
        usbReceiver,
        filter,
        null,
        handler,
        Context.RECEIVER_NOT_EXPORTED
      )
    } catch (e: SecurityException) {
      Log.w(TAG, "Failed to register receiver with RECEIVER_NOT_EXPORTED flag, falling back", e)
      @Suppress("DEPRECATION")
      context.registerReceiver(usbReceiver, filter)
    }

    // Register MIDI device callback
    midiManager.registerDeviceCallback(deviceCallback, handler)
    
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
      Log.d(TAG, "Refreshing available MIDI devices")
      
      // Get MIDI devices from MidiManager
      @Suppress("DEPRECATION")
      val midiDevices = midiManager?.devices ?: emptyArray()
      
      if (midiDevices.isEmpty()) {
        Log.d(TAG, "No MIDI devices found via MidiManager")
        notifyListeners(ConnectionState.DISCONNECTED, "No MIDI devices found")
        return
      }
      
      // Log all MIDI devices
      midiDevices.forEach { device ->
        val properties = device.properties
        val name = properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown"
        val manufacturer = properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) ?: "Unknown"
        val product = properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT) ?: "Unknown"
        
        Log.d(TAG, """
          MIDI Device:
          Name: $name
          Manufacturer: $manufacturer
          Product: $product
          Type: ${device.type}
          Input Ports: ${device.inputPortCount}
          Output Ports: ${device.outputPortCount}
        """.trimIndent())
      }
      
      // Try to connect to the first available device
      if (currentDevice == null && midiDevices.isNotEmpty()) {
        handleMidiDevice(midiDevices[0])
      }
      
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing MIDI devices", e)
      notifyListeners(ConnectionState.ERROR, "Error refreshing devices: ${e.message}")
    }
  }

  override fun connectToUsbDevice(device: UsbDevice) {
    try {
      Log.d(TAG, "Attempting to connect to USB device: ${device.deviceName}")
      
      // First, disconnect from any existing device
      if (currentDevice != null) {
        disconnect()
      }
      
      // Request permission if we don't have it
      if (!usbManager.hasPermission(device)) {
        Log.d(TAG, "Requesting USB permission for device: ${device.deviceName}")
        
        val permissionIntent = PendingIntent.getBroadcast(
          context,
          0,
          Intent(ACTION_USB_PERMISSION),
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
          } else {
            PendingIntent.FLAG_UPDATE_CURRENT
          }
        )
        
        usbManager.requestPermission(device, permissionIntent)
        notifyListeners(ConnectionState.CONNECTING, "Requesting USB permission...")
      } else {
        Log.d(TAG, "Already have permission for device, connecting...")
        connectToMidiDevice(device)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to USB device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting: ${e.message}")
    }
  }

  private fun connectToMidiDevice(device: UsbDevice) {
    try {
      Log.d(TAG, "Connecting to MIDI device: ${device.deviceName}")
      notifyListeners(ConnectionState.CONNECTING, "Connecting to ${device.deviceName}...")

      // Simplified connection strategy
      var midiInterface: android.hardware.usb.UsbInterface? = null
      for (i in 0 until device.interfaceCount) {
        val intf = device.getInterface(i)
        if (intf.interfaceClass == UsbConstants.USB_CLASS_AUDIO && intf.interfaceSubclass == UsbConstants.USB_SUBCLASS_MIDISTREAMING) {
          midiInterface = intf
          break
        }
      }

      if (midiInterface == null) {
        Log.e(TAG, "No MIDI interface found on device")
        notifyListeners(ConnectionState.ERROR, "No MIDI interface found on device")
        return
      }

      val connection = usbManager.openDevice(device)
      if (connection == null) {
        Log.e(TAG, "Failed to open device connection")
        notifyListeners(ConnectionState.ERROR, "Failed to open device connection")
        return
      }

      if (!connection.claimInterface(midiInterface, true)) {
        Log.e(TAG, "Failed to claim interface")
        connection.close()
        notifyListeners(ConnectionState.ERROR, "Failed to claim interface")
        return
      }

      directUsbConnection = connection
      directUsbInterface = midiInterface

      // Improved error handling
      val devices = midiManager?.devices ?: emptyArray()
      var foundMidiDevice = false

      for (midiDeviceInfo in devices) {
        val props = midiDeviceInfo.properties
        val midiDeviceName = props.getString(MidiDeviceInfo.PROPERTY_NAME) ?: ""

        if (midiDeviceName.contains(device.deviceName, ignoreCase = true)) {
          midiManager?.openDevice(midiDeviceInfo, { midiDevice ->
            if (midiDevice != null) {
              currentDevice = midiDevice
              currentDeviceInfo = midiDeviceInfo
              setupMidiInput(midiDevice)
              foundMidiDevice = true
            } else {
              Log.e(TAG, "Failed to open MIDI device")
              setupDirectConnection(device, connection, midiInterface)
            }
          }, Handler(Looper.getMainLooper()))
          break
        }
      }

      if (!foundMidiDevice) {
        setupDirectConnection(device, connection, midiInterface)
      }

    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to MIDI device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting: ${e.message}")
    }
  }

  private fun setupMidiInput(device: MidiDevice) {
    try {
      if (device.info.inputPortCount == 0) {
        Log.e(TAG, "Device has no input ports")
        notifyListeners(ConnectionState.ERROR, "Device has no input ports")
        return
      }

      val inputPort = device.openInputPort(0)
      if (inputPort != null) {
        currentInputPort = inputPort
        midiInputProcessor.setInputPort(inputPort)

        Log.d(TAG, "MIDI input port opened successfully")
        notifyListeners(ConnectionState.CONNECTED, "Connected to ${device.info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
      } else {
        Log.e(TAG, "Failed to open MIDI input port")
        notifyListeners(ConnectionState.ERROR, "Failed to open MIDI input port")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error setting up MIDI input", e)
      notifyListeners(ConnectionState.ERROR, "Error setting up MIDI input: ${e.message}")
    }
  }

  override fun addMidiEventListener(listener: MidiEventListener) {
    midiInputProcessor.registerListener(listener)
    Log.d(TAG, "Added MIDI event listener: $listener")
  }

  override fun removeMidiEventListener(listener: MidiEventListener) {
    midiInputProcessor.unregisterListener(listener)
    Log.d(TAG, "Removed MIDI event listener: $listener")
  }

  override fun disconnect() {
    try {
      Log.d(TAG, "Disconnecting MIDI device")
      directConnectionThread?.interrupt()
      directConnectionThread = null

      directUsbConnection?.apply {
        directUsbInterface?.let { releaseInterface(it) }
        close()
      }
      directUsbConnection = null
      directUsbInterface = null

      currentInputPort?.close()
      currentInputPort = null

      currentDevice?.close()
      currentDevice = null
      currentDeviceInfo = null

      notifyListeners(ConnectionState.DISCONNECTED, "Disconnected")
    } catch (e: Exception) {
      Log.e(TAG, "Error disconnecting", e)
      notifyListeners(ConnectionState.ERROR, "Error disconnecting: ${e.message ?: "Unknown error"}")
    }
  }

  override fun cleanup() {
    try {
      Log.d(TAG, "Cleaning up MIDI device manager")
      disconnect()
      midiManager?.unregisterDeviceCallback(deviceCallback)
      context.unregisterReceiver(usbReceiver)
      listeners.clear()
      midiInputProcessor.cleanup()
      Log.d(TAG, "Cleanup completed")
    } catch (e: Exception) {
      Log.e(TAG, "Error during cleanup", e)
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

  private fun handleMidiDevice(deviceInfo: MidiDeviceInfo) {
    try {
      val properties = deviceInfo.properties
      val deviceName = properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown MIDI Device"
      
      Log.d(TAG, "Handling MIDI device: $deviceName")
      
      // Check if this is a USB device
      if (deviceInfo.type == MidiDeviceInfo.TYPE_USB) {
        Log.d(TAG, "This is a USB MIDI device")
        connectToDevice(deviceInfo)
      } else {
        Log.d(TAG, "This is not a USB MIDI device (type: ${deviceInfo.type})")
        // We'll connect anyway since it might be a virtual MIDI device
        connectToDevice(deviceInfo)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error handling MIDI device", e)
      notifyListeners(ConnectionState.ERROR, "Error handling MIDI device: ${e.message}")
    }
  }

  private fun connectToDevice(deviceInfo: MidiDeviceInfo) {
    try {
      val deviceName = deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown Device"
      Log.d(TAG, "Connecting to MIDI device: $deviceName")
      
      notifyListeners(ConnectionState.CONNECTING, "Connecting to $deviceName...")
      
      midiManager?.openDevice(deviceInfo, { device ->
        if (device != null) {
          Log.d(TAG, "Successfully opened MIDI device")
          currentDevice = device
          currentDeviceInfo = deviceInfo
          setupMidiInput(device)
        } else {
          Log.e(TAG, "Failed to open MIDI device - device is null")
          stopRetrying()
          notifyListeners(ConnectionState.ERROR, "Failed to open MIDI device")
        }
      }, handler)
      
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to MIDI device", e)
      stopRetrying()
      notifyListeners(ConnectionState.ERROR, "Error connecting to device: ${e.message}")
    }
  }

  private fun setupDirectConnection(device: UsbDevice, connection: android.hardware.usb.UsbDeviceConnection, midiInterface: android.hardware.usb.UsbInterface) {
    try {
      Log.d(TAG, "Setting up direct USB connection")
      
      // Find the bulk input endpoint
      var inputEndpoint: android.hardware.usb.UsbEndpoint? = null
      for (i in 0 until midiInterface.endpointCount) {
        val endpoint = midiInterface.getEndpoint(i)
        if (endpoint.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK &&
            endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) {
          inputEndpoint = endpoint
          Log.d(TAG, "Found bulk input endpoint: ${endpoint.endpointNumber}")
          break
        }
      }
      
      if (inputEndpoint == null) {
        Log.e(TAG, "No input endpoint found")
        notifyListeners(ConnectionState.ERROR, "No input endpoint found")
        connection.releaseInterface(midiInterface)
        connection.close()
        return
      }
      
      // Start a thread to read from the USB device
      val running = AtomicBoolean(true)
      directConnectionThread = Thread {
        Log.d(TAG, "Starting direct USB read thread")
        val buffer = ByteArray(64) // Buffer size for MIDI data
        
        while (running.get()) {
          try {
            val bytesRead = connection.bulkTransfer(inputEndpoint, buffer, buffer.size, 100)
            if (bytesRead > 0) {
              Log.d(TAG, "Read $bytesRead bytes from USB device")
              
              // Process the MIDI data
              midiInputProcessor.processMidiData(buffer, 0, bytesRead, System.nanoTime())
            }
          } catch (e: Exception) {
            if (running.get()) {
              Log.e(TAG, "Error reading from USB device", e)
            }
          }
        }
        
        Log.d(TAG, "Direct USB read thread stopped")
      }.apply {
        name = "MidiDeviceManagerDirectThread"
        isDaemon = true
        start()
      }
      
      notifyListeners(ConnectionState.CONNECTED, "Connected to ${device.deviceName}")
      
    } catch (e: Exception) {
      Log.e(TAG, "Error setting up direct connection", e)
      notifyListeners(ConnectionState.ERROR, "Error setting up direct connection: ${e.message}")
    }
  }

  private fun notifyListeners(state: ConnectionState, message: String) {
    handler.post {
      listeners.forEach { it.onConnectionStateChanged(state, message) }
    }
  }

  // Add USB constants if not imported
  private object UsbConstants {
    const val USB_CLASS_AUDIO = 0x01
    const val USB_CLASS_VENDOR_SPEC = 0xFF
    const val USB_SUBCLASS_MIDISTREAMING = 0x03
  }
}
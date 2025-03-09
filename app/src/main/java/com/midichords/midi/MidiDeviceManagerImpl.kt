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
                notifyListeners(ConnectionState.CONNECTING, "Permission granted for ${it.deviceName}")
                connectToUsbDevice(it)
              } ?: run {
                Log.e(TAG, "Permission granted but device is null")
                notifyListeners(ConnectionState.ERROR, "Permission granted but device is null")
              }
            } else {
              device?.let {
                Log.e(TAG, "USB permission denied for device: ${it.deviceName}")
                notifyListeners(ConnectionState.ERROR, "USB permission denied for ${it.deviceName}. Please reconnect the device and try again.")
              } ?: run {
                Log.e(TAG, "USB permission denied for unknown device")
                notifyListeners(ConnectionState.ERROR, "USB permission denied. Please reconnect the device and try again.")
              }
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
            notifyListeners(ConnectionState.CONNECTING, "USB device attached: ${it.deviceName}")
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
            notifyListeners(ConnectionState.DISCONNECTED, "USB device detached: ${it.deviceName}")
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

    try {
      context.registerReceiver(
        usbReceiver,
        filter,
        null,
        Handler(context.mainLooper),
        Context.RECEIVER_NOT_EXPORTED
      )
    } catch (e: SecurityException) {
      // Fall back to the older registration method for compatibility
      Log.w(TAG, "Failed to register receiver with RECEIVER_NOT_EXPORTED flag, falling back", e)
      try {
        @Suppress("DEPRECATION")
        context.registerReceiver(
          usbReceiver,
          filter,
          null,
          Handler(context.mainLooper)
        )
      } catch (e2: Exception) {
        Log.e(TAG, "Failed to register receiver even with fallback method", e2)
        throw IllegalStateException("Could not register USB receiver: ${e2.message}")
      }
    }

    Log.d(TAG, "MidiDeviceManager initialized")
    
    // Log all USB devices at initialization
    logAllUsbDevices()
  }

  override fun registerListener(listener: MidiDeviceListener) {
    listeners.add(listener)
  }

  override fun unregisterListener(listener: MidiDeviceListener) {
    listeners.remove(listener)
  }

  override fun refreshAvailableDevices() {
    try {
      // First, check for USB devices directly
      val allUsbDevices = usbManager.deviceList.values.toList()
      
      // Completely exclude the 0xBDA converter device
      val filteredDevices = allUsbDevices.filter { device -> 
        device.vendorId != 0x0BDA 
      }
      
      Log.d(TAG, "Found ${allUsbDevices.size} total USB devices, ${filteredDevices.size} after filtering out converters")
      
      if (filteredDevices.isNotEmpty()) {
        // Select the first non-converter device
        val selectedDevice = filteredDevices.first()
        val vendorId = "0x${selectedDevice.vendorId.toString(16).uppercase()}"
        val productId = "0x${selectedDevice.productId.toString(16).uppercase()}"
        
        Log.d(TAG, "Selected device: ${selectedDevice.deviceName} (VID:$vendorId, PID:$productId)")
        notifyListeners(ConnectionState.CONNECTING, "Connecting to device: ${selectedDevice.deviceName} (VID:$vendorId, PID:$productId)")
        requestUsbPermission(selectedDevice)
        return
      } else if (allUsbDevices.isNotEmpty()) {
        // If we only have converter devices, show a message
        Log.d(TAG, "Only found USB converter devices, no MIDI devices")
        notifyListeners(ConnectionState.ERROR, "Only found USB converter devices. Please connect a MIDI device directly.")
        return
      }
      
      // If no USB devices found, try the MIDI manager approach
      @Suppress("DEPRECATION")
      val devices = midiManager?.devices ?: emptyArray()
      handleDeviceList(devices)
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing devices", e)
      notifyListeners(ConnectionState.ERROR, "Error refreshing devices: ${e.message}")
    }
  }
  
  /**
   * Check for USB devices directly using UsbManager
   */
  private fun checkForUsbDevices(): List<UsbDevice> {
    val deviceList = usbManager.deviceList
    Log.d(TAG, "Found ${deviceList.size} USB devices via UsbManager")
    
    val midiDevices = mutableListOf<UsbDevice>()
    val converterDevices = mutableListOf<UsbDevice>()
    
    // Known USB converter/hub vendor IDs to filter out
    val knownConverterVendors = listOf(
      0x0BDA, // Realtek (common in USB-C adapters)
      0x05AC, // Apple
      0x2109, // VIA Labs (USB hubs)
      0x1A40, // Terminus Technology (USB hubs)
      0x0424, // Standard Microsystems Corp (USB hubs)
      0x0451, // Texas Instruments (USB hubs)
      0x174C, // ASMedia (USB hubs)
      0x8087  // Intel (USB hubs)
    )
    
    // Known MIDI device vendor IDs to prioritize
    val knownMidiVendors = listOf(
      0x041E, // Creative Labs
      0x0763, // M-Audio
      0x0D8C, // C-Media (some MIDI adapters)
      0x1397, // BEHRINGER
      0x152A, // Thesycon (MIDI driver)
      0x1A86, // QinHeng (CH345/CH9325 USB-MIDI adapters)
      0x2982, // Akai
      0x07CF, // Casio
      0x0582, // Roland
      0x0944, // Korg
      0x1410, // Novation
      0x17CC, // Native Instruments
      0x0499  // Yamaha
    )
    
    // Log details about each device
    deviceList.forEach { (name, device) ->
      Log.d(TAG, "=== USB Device: $name ===")
      Log.d(TAG, "  Device ID: ${device.deviceId}")
      Log.d(TAG, "  Product ID: 0x${device.productId.toString(16).uppercase()} (${device.productId})")
      Log.d(TAG, "  Vendor ID: 0x${device.vendorId.toString(16).uppercase()} (${device.vendorId})")
      Log.d(TAG, "  Device Class: ${device.deviceClass}")
      Log.d(TAG, "  Device Subclass: ${device.deviceSubclass}")
      Log.d(TAG, "  Interface Count: ${device.interfaceCount}")
      
      // Check if this device has a MIDI interface
      var hasMidiInterface = false
      var potentialMidiInterface = false
      
      // Log all interfaces
      for (i in 0 until device.interfaceCount) {
        val usbInterface = device.getInterface(i)
        val interfaceClass = usbInterface.interfaceClass
        val interfaceSubclass = usbInterface.interfaceSubclass
        
        Log.d(TAG, "  Interface $i: Class ${interfaceClass} (0x${interfaceClass.toString(16)}), Subclass ${interfaceSubclass} (0x${interfaceSubclass.toString(16)})")
        
        // Log all endpoints for this interface
        for (j in 0 until usbInterface.endpointCount) {
          val endpoint = usbInterface.getEndpoint(j)
          val direction = if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) "IN" else "OUT"
          val type = when (endpoint.type) {
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
            else -> "UNKNOWN"
          }
          Log.d(TAG, "    Endpoint $j: Address 0x${endpoint.address.toString(16)}, Type $type, Direction $direction")
        }
        
        if (interfaceClass == 1 && interfaceSubclass == 3) {
          hasMidiInterface = true
          Log.d(TAG, "  Interface $i is a standard MIDI interface (Class 1, Subclass 3)")
        }
        // Check for other common MIDI interface patterns
        else if (interfaceClass == 2 && interfaceSubclass == 6) {
          // Some MIDI devices use Communications class (2) with subclass 6
          potentialMidiInterface = true
          Log.d(TAG, "  Interface $i is likely a MIDI interface (Class 2, Subclass 6)")
        }
        else if (interfaceClass == 255) {
          // Vendor-specific class, might be MIDI
          potentialMidiInterface = true
          Log.d(TAG, "  Interface $i is a vendor-specific interface (Class 255)")
        }
      }
      
      // Determine if this is a converter/hub or a MIDI device
      val isConverter = device.vendorId in knownConverterVendors || 
                        (device.deviceClass == 9) || // Hub class
                        (device.deviceClass == 0 && device.deviceSubclass == 0 && device.interfaceCount == 1 && !hasMidiInterface && !potentialMidiInterface)
      
      val isMidiDevice = hasMidiInterface || 
                         potentialMidiInterface || 
                         device.vendorId in knownMidiVendors
      
      if (isConverter) {
        Log.d(TAG, "  This device appears to be a USB converter/hub, not a MIDI device")
        converterDevices.add(device)
      } else if (isMidiDevice) {
        if (hasMidiInterface) {
          Log.d(TAG, "  This device has a standard MIDI interface")
        } else if (potentialMidiInterface) {
          Log.d(TAG, "  This device has a potential MIDI interface (non-standard)")
        } else {
          Log.d(TAG, "  This device has a known MIDI vendor ID: 0x${device.vendorId.toString(16).uppercase()}")
        }
        midiDevices.add(device)
      } else {
        Log.d(TAG, "  This device does not appear to be a MIDI device or converter")
      }
      
      Log.d(TAG, "===================")
    }
    
    // If we found MIDI devices, return those
    if (midiDevices.isNotEmpty()) {
      Log.d(TAG, "Found ${midiDevices.size} potential MIDI devices among USB devices")
      midiDevices.forEach { device ->
        Log.d(TAG, "  Potential MIDI device: ${device.deviceName}, Vendor: 0x${device.vendorId.toString(16).uppercase()}, Product: 0x${device.productId.toString(16).uppercase()}")
      }
      return midiDevices
    }
    
    // If we only found converters, return those as a last resort
    if (converterDevices.isNotEmpty()) {
      Log.d(TAG, "Found only USB converters/hubs, returning as last resort")
      return converterDevices
    }
    
    Log.d(TAG, "No MIDI devices or converters found among USB devices")
    return emptyList()
  }
  
  /**
   * Log all USB devices for debugging purposes
   */
  private fun logAllUsbDevices() {
    Log.d(TAG, "=== USB DEVICES ===")
    val deviceList = usbManager.deviceList
    if (deviceList.isEmpty()) {
      Log.d(TAG, "No USB devices found")
    } else {
      deviceList.forEach { (name, device) ->
        Log.d(TAG, "USB Device: $name")
        Log.d(TAG, "  Device ID: ${device.deviceId}")
        Log.d(TAG, "  Product ID: ${device.productId}")
        Log.d(TAG, "  Vendor ID: ${device.vendorId}")
      }
    }
    
    Log.d(TAG, "=== MIDI DEVICES ===")
    @Suppress("DEPRECATION")
    val midiDevices = midiManager?.devices ?: emptyArray()
    if (midiDevices.isEmpty()) {
      Log.d(TAG, "No MIDI devices found")
    } else {
      midiDevices.forEach { deviceInfo ->
        val properties = deviceInfo.properties
        Log.d(TAG, "MIDI Device: ${properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
        Log.d(TAG, "  Type: ${if (deviceInfo.type == MidiDeviceInfo.TYPE_USB) "USB" else "Other"}")
        if (deviceInfo.type == MidiDeviceInfo.TYPE_USB) {
          Log.d(TAG, "  Device ID: ${properties.getInt(MidiDeviceInfo.PROPERTY_USB_DEVICE)}")
        }
      }
    }
    Log.d(TAG, "==================")
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
      Log.d(TAG, "Attempting to connect to MIDI device: ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
      midiManager?.openDevice(deviceInfo, { device ->
        if (device != null) {
          currentDevice = device
          currentDeviceInfo = deviceInfo
          setupMidiInput(device)
          notifyListeners(ConnectionState.CONNECTED, "Connected to ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
        } else {
          Log.e(TAG, "Failed to open MIDI device - device is null")
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
      Log.d(TAG, "Setting up MIDI input for device: ${currentDeviceInfo?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)}")
      Log.d(TAG, "Device has ${device.info.inputPortCount} input ports and ${device.info.outputPortCount} output ports")
      
      if (device.info.inputPortCount == 0) {
        Log.e(TAG, "Device has no input ports")
        notifyListeners(ConnectionState.ERROR, "Device has no input ports")
        startRetrying()
        return
      }
      
      val inputPort = device.openInputPort(0)
      if (inputPort != null) {
        // Store the input port
        currentInputPort = inputPort
        
        // Store the input port in the processor
        midiInputProcessor.setInputPort(inputPort)
        
        // For MIDI input ports, we don't need to establish a connection
        // The device will send MIDI data to our app, and we'll process it in the MidiReceiver
        
        Log.d(TAG, "MIDI input port opened successfully")
        notifyListeners(ConnectionState.CONNECTED, "Connected to ${currentDeviceInfo?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)}")
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
      Log.d(TAG, "Attempting to connect to USB device: ${device.deviceName}")
      
      // First check if we have permission
      if (!usbManager.hasPermission(device)) {
        Log.d(TAG, "No permission for USB device, requesting permission")
        requestUsbPermission(device)
        return
      }
      
      // Check if this device has a MIDI interface
      var hasMidiInterface = false
      for (i in 0 until device.interfaceCount) {
        val usbInterface = device.getInterface(i)
        if (usbInterface.interfaceClass == 1 && usbInterface.interfaceSubclass == 3) {
          hasMidiInterface = true
          Log.d(TAG, "Device has a standard MIDI interface (Class 1, Subclass 3)")
          break
        }
        // Check for other common MIDI interface patterns
        else if (usbInterface.interfaceClass == 2 && usbInterface.interfaceSubclass == 6) {
          // Some MIDI devices use Communications class (2) with subclass 6
          hasMidiInterface = true
          Log.d(TAG, "Device has a likely MIDI interface (Class 2, Subclass 6)")
          break
        }
        else if (usbInterface.interfaceClass == 255) {
          // Vendor-specific class, might be MIDI
          Log.d(TAG, "Device has a vendor-specific interface (Class 255) that might be MIDI")
          hasMidiInterface = true
          break
        }
      }
      
      if (!hasMidiInterface) {
        Log.e(TAG, "Device does not appear to have a MIDI interface")
        notifyListeners(ConnectionState.ERROR, "Device does not appear to have a standard MIDI interface, but we'll try anyway")
        // We'll continue anyway since some devices don't properly report their interfaces
      }
      
      // Try to find the device in the MIDI manager
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
    Log.d(TAG, "Searching for USB device ${usbDevice.deviceName} in ${devices.size} MIDI devices")
    
    for (deviceInfo in devices) {
      if (deviceInfo.type == MidiDeviceInfo.TYPE_USB) {
        val properties = deviceInfo.properties
        val deviceId = properties.getInt(MidiDeviceInfo.PROPERTY_USB_DEVICE)
        Log.d(TAG, "Checking MIDI device with ID $deviceId against USB device ID ${usbDevice.deviceId}")
        
        if (deviceId == usbDevice.deviceId) {
          Log.d(TAG, "Found matching MIDI device for USB device ${usbDevice.deviceName}")
          connectToDevice(deviceInfo)
          return
        }
      }
    }
    
    Log.d(TAG, "USB device not recognized as MIDI device by Android MIDI service.")
    Log.d(TAG, "This could be because:")
    Log.d(TAG, "1. The device is not a MIDI device")
    Log.d(TAG, "2. The device requires a driver that is not installed")
    Log.d(TAG, "3. The Android MIDI service has not yet recognized the device")
    
    // Try a direct approach for devices that aren't recognized by the MIDI service
    tryDirectConnection(usbDevice)
  }
  
  private var directUsbConnection: android.hardware.usb.UsbDeviceConnection? = null
  private var directUsbInterface: android.hardware.usb.UsbInterface? = null
  private var directConnectionThread: Thread? = null
  
  private fun tryDirectConnection(device: UsbDevice) {
    Log.d(TAG, "Attempting direct connection to USB device: ${device.deviceName}")
    notifyListeners(ConnectionState.CONNECTING, "Attempting direct connection to ${device.deviceName}")
    
    try {
      // Open a connection to the device
      val connection = usbManager.openDevice(device)
      if (connection == null) {
        Log.e(TAG, "Failed to open connection to device: ${device.deviceName}")
        notifyListeners(ConnectionState.ERROR, "Failed to open connection to device. Make sure you've granted permission.")
        return
      }
      
      Log.d(TAG, "Successfully opened connection to device: ${device.deviceName}")
      
      // Find a suitable interface to claim
      var usbInterface: android.hardware.usb.UsbInterface? = null
      var selectedInterfaceIndex = -1
      
      // First, try to find a MIDI interface (class 1, subclass 3)
      for (i in 0 until device.interfaceCount) {
        val intf = device.getInterface(i)
        if (intf.interfaceClass == 1 && intf.interfaceSubclass == 3) {
          usbInterface = intf
          selectedInterfaceIndex = i
          Log.d(TAG, "Found standard MIDI interface at index $i")
          break
        }
      }
      
      // If no standard MIDI interface, try communications class (2, 6)
      if (usbInterface == null) {
        for (i in 0 until device.interfaceCount) {
          val intf = device.getInterface(i)
          if (intf.interfaceClass == 2 && intf.interfaceSubclass == 6) {
            usbInterface = intf
            selectedInterfaceIndex = i
            Log.d(TAG, "Found communications interface at index $i that might be MIDI")
            break
          }
        }
      }
      
      // If still no interface, try vendor-specific (255)
      if (usbInterface == null) {
        for (i in 0 until device.interfaceCount) {
          val intf = device.getInterface(i)
          if (intf.interfaceClass == 255) {
            usbInterface = intf
            selectedInterfaceIndex = i
            Log.d(TAG, "Found vendor-specific interface at index $i")
            break
          }
        }
      }
      
      // If still no interface, try audio class (1) with any subclass
      if (usbInterface == null) {
        for (i in 0 until device.interfaceCount) {
          val intf = device.getInterface(i)
          if (intf.interfaceClass == 1) {
            usbInterface = intf
            selectedInterfaceIndex = i
            Log.d(TAG, "Found audio interface at index $i with subclass ${intf.interfaceSubclass}")
            break
          }
        }
      }
      
      // If still no interface, just use the first one
      if (usbInterface == null && device.interfaceCount > 0) {
        usbInterface = device.getInterface(0)
        selectedInterfaceIndex = 0
        Log.d(TAG, "Using first available interface as fallback")
      }
      
      if (usbInterface == null) {
        Log.e(TAG, "No suitable interface found on device: ${device.deviceName}")
        connection.close()
        notifyListeners(ConnectionState.ERROR, "No suitable interface found on device")
        return
      }
      
      Log.d(TAG, "Selected interface $selectedInterfaceIndex: Class ${usbInterface.interfaceClass}, Subclass ${usbInterface.interfaceSubclass}")
      
      // Claim the interface
      val claimed = connection.claimInterface(usbInterface, true)
      if (!claimed) {
        Log.e(TAG, "Failed to claim interface on device: ${device.deviceName}")
        connection.close()
        notifyListeners(ConnectionState.ERROR, "Failed to claim interface on device")
        return
      }
      
      Log.d(TAG, "Successfully claimed interface on device: ${device.deviceName}")
      
      // Store the connection and interface for later cleanup
      directUsbConnection = connection
      directUsbInterface = usbInterface
      
      // Find an input endpoint (direction IN)
      var inputEndpoint: android.hardware.usb.UsbEndpoint? = null
      
      // First try to find an INTERRUPT or BULK IN endpoint
      for (i in 0 until usbInterface.endpointCount) {
        val endpoint = usbInterface.getEndpoint(i)
        val direction = if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) "IN" else "OUT"
        val type = when (endpoint.type) {
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
          else -> "UNKNOWN"
        }
        
        Log.d(TAG, "Endpoint $i: Address 0x${endpoint.address.toString(16)}, Type $type, Direction $direction")
        
        if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) {
          if (endpoint.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT) {
            // Interrupt endpoints are preferred for MIDI
            inputEndpoint = endpoint
            Log.d(TAG, "Found INTERRUPT IN endpoint at index $i")
            break
          } else if (endpoint.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK && inputEndpoint == null) {
            // Bulk endpoints are also common for MIDI
            inputEndpoint = endpoint
            Log.d(TAG, "Found BULK IN endpoint at index $i")
            // Don't break, continue looking for an INTERRUPT endpoint
          }
        }
      }
      
      // If no INTERRUPT or BULK endpoint found, try any IN endpoint
      if (inputEndpoint == null) {
        for (i in 0 until usbInterface.endpointCount) {
          val endpoint = usbInterface.getEndpoint(i)
          if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) {
            inputEndpoint = endpoint
            Log.d(TAG, "Found IN endpoint at index $i (type: ${endpoint.type})")
            break
          }
        }
      }
      
      if (inputEndpoint == null) {
        Log.e(TAG, "No input endpoint found on interface")
        connection.releaseInterface(usbInterface)
        connection.close()
        directUsbConnection = null
        directUsbInterface = null
        notifyListeners(ConnectionState.ERROR, "No input endpoint found on device")
        return
      }
      
      // Notify that we're connected
      notifyListeners(ConnectionState.CONNECTED, "Connected directly to ${device.deviceName}")
      
      // Start a thread to read from the device
      val thread = Thread {
        try {
          val buffer = ByteArray(64) // Buffer for reading data
          
          Log.d(TAG, "Starting USB read loop")
          while (!Thread.interrupted()) {
            // Read from the endpoint
            val bytesRead = connection.bulkTransfer(inputEndpoint, buffer, buffer.size, 100)
            
            if (bytesRead > 0) {
              // Log the raw bytes for debugging
              val hexData = buffer.slice(0 until bytesRead).joinToString(" ") { 
                "0x${it.toInt().and(0xFF).toString(16).padStart(2, '0')}" 
              }
              Log.d(TAG, "Read $bytesRead bytes from USB device: $hexData")
              
              // Process the MIDI data
              val timestamp = System.nanoTime()
              midiInputProcessor.processMidiData(buffer, 0, bytesRead, timestamp)?.let { event ->
                // Log the event
                Log.d(TAG, "Processed MIDI event from direct USB: ${event.type}, " +
                           "channel: ${event.channel}, data1: ${event.data1}, data2: ${event.data2}")
              }
            }
            
            // Small delay to avoid hammering the CPU
            Thread.sleep(5)
          }
        } catch (e: Exception) {
          if (e is InterruptedException) {
            // Thread was interrupted, this is expected during cleanup
            Log.d(TAG, "USB read thread interrupted")
          } else {
            Log.e(TAG, "Error reading from USB device", e)
            notifyListeners(ConnectionState.ERROR, "Error reading from USB device: ${e.message}")
          }
        }
      }
      
      directConnectionThread = thread
      thread.start()
      
    } catch (e: Exception) {
      Log.e(TAG, "Error in direct USB connection", e)
      notifyListeners(ConnectionState.ERROR, "Error in direct USB connection: ${e.message}")
      
      // If we have a USB device but it's not in the MIDI devices list,
      // we'll try again after a short delay to give the MIDI service time to recognize it
      handler.postDelayed({
        Log.d(TAG, "Retrying MIDI device detection after delay")
        refreshAvailableDevices()
      }, 2000)
    }
  }

  private fun requestUsbPermission(device: UsbDevice) {
    Log.d(TAG, "Requesting permission for USB device: ${device.deviceName}")
    notifyListeners(ConnectionState.CONNECTING, "Requesting permission for USB device: ${device.deviceName}")
    
    try {
      // Check if we already have permission
      if (usbManager.hasPermission(device)) {
        Log.d(TAG, "Already have permission for USB device: ${device.deviceName}")
        connectToUsbDevice(device)
        return
      }
      
      // Create a PendingIntent for the permission request
      val permissionIntent = PendingIntent.getBroadcast(
        context,
        device.deviceId, // Use device ID as request code to differentiate between devices
        Intent(ACTION_USB_PERMISSION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )
      
      // Request permission
      usbManager.requestPermission(device, permissionIntent)
      Log.d(TAG, "Permission request sent for device: ${device.deviceName}")
      
      // Set a timeout to check if permission was granted
      handler.postDelayed({
        if (!usbManager.hasPermission(device)) {
          Log.d(TAG, "Permission request timed out for device: ${device.deviceName}")
          notifyListeners(ConnectionState.ERROR, "USB permission request timed out. Please try again.")
        }
      }, 10000) // 10 second timeout
    } catch (e: Exception) {
      Log.e(TAG, "Error requesting USB permission", e)
      notifyListeners(ConnectionState.ERROR, "Error requesting USB permission: ${e.message}")
    }
  }

  override fun disconnect() {
    try {
      // Clean up MIDI connection
      currentInputPort?.close()
      currentInputPort = null
      currentDevice?.close()
      currentDevice = null
      currentDeviceInfo = null
      
      // Clean up direct USB connection
      directConnectionThread?.interrupt()
      directConnectionThread = null
      
      if (directUsbInterface != null && directUsbConnection != null) {
        try {
          directUsbConnection?.releaseInterface(directUsbInterface)
          directUsbConnection?.close()
        } catch (e: Exception) {
          Log.e(TAG, "Error releasing USB interface", e)
        }
      }
      directUsbInterface = null
      directUsbConnection = null
      
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
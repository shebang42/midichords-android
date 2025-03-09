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
            
            // HARD BLOCK: Check if this is a 0xBDA device
            if (device != null && isBlockedConverter(device)) {
              Log.e(TAG, "HARD BLOCK: Received permission result for 0xBDA converter device: ${device.deviceName}")
              notifyListeners(ConnectionState.ERROR, "Cannot connect to USB converter device (0xBDA). Please connect a MIDI device directly.")
              return
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
          
          // HARD BLOCK: Check if this is a 0xBDA device
          if (device != null && isBlockedConverter(device)) {
            Log.e(TAG, "HARD BLOCK: 0xBDA converter device attached: ${device.deviceName}")
            notifyListeners(ConnectionState.ERROR, "USB converter device (0xBDA) attached. Please connect a MIDI device directly.")
            return
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

  /**
   * Check if a device is a 0xBDA converter that should be blocked
   */
  private fun isBlockedConverter(device: UsbDevice): Boolean {
    val isBlocked = device.vendorId == 0x0BDA
    if (isBlocked) {
      Log.e(TAG, "BLOCKED DEVICE: ${device.deviceName} has vendor ID 0xBDA (Realtek converter)")
    }
    return isBlocked
  }

  override fun refreshAvailableDevices() {
    try {
      // Get the USB service
      val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager?
      if (usbManager == null) {
        Log.e(TAG, "Failed to get USB service")
        notifyListeners(ConnectionState.ERROR, "Failed to get USB service")
        return
      }
      
      // Get all connected devices
      val deviceList = usbManager.deviceList
      Log.d(TAG, "Found ${deviceList.size} USB devices")
      
      // Log detailed info about all connected USB devices for debugging
      logAllUsbDevices(deviceList)
      
      // Filter out unwanted devices
      val filteredDevices = deviceList.values.filter { device ->
        // Get the vendor ID
        val vendorId = device.vendorId
        
        // Print detailed logs for each device
        Log.d(TAG, "Examining USB device: ${device.deviceName}, " +
              "vendorId=0x${vendorId.toString(16)}, " +
              "productId=0x${device.productId.toString(16)}, " +
              "manufacturerName=${device.manufacturerName}, " +
              "productName=${device.productName}, " +
              "deviceClass=${device.deviceClass}, " +
              "interfaceCount=${device.interfaceCount}")
        
        // Skip the USB converter with vendor ID 0xBDA
        if (vendorId == 0xBDA) {
          Log.d(TAG, "Skipping known USB converter with vendor ID 0xBDA: ${device.deviceName}")
          return@filter false
        }
        
        // Consider any other USB device
        true
      }
      
      Log.d(TAG, "After filtering: ${filteredDevices.size} USB devices")
      
      // If we don't have any devices, notify the user
      if (filteredDevices.isEmpty()) {
        Log.d(TAG, "No USB devices found after filtering")
        notifyListeners(ConnectionState.DISCONNECTED, "No compatible USB devices found")
        return
      }
      
      // Take the first device
      val device = filteredDevices.first()
      Log.d(TAG, "Selected USB device: ${device.deviceName}, " +
            "vendorId=0x${device.vendorId.toString(16)}, " +
            "productId=0x${device.productId.toString(16)}")
      
      // If we have a device, try to connect
      connectToUsbDevice(device)
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing available devices", e)
      notifyListeners(ConnectionState.ERROR, "Error finding devices: ${e.message}")
    }
  }
  
  /**
   * Log detailed information about all connected USB devices
   */
  private fun logAllUsbDevices(deviceList: HashMap<String, UsbDevice>) {
    Log.d(TAG, "========== USB DEVICE DETAILS ==========")
    
    if (deviceList.isEmpty()) {
      Log.d(TAG, "No USB devices connected")
      return
    }
    
    deviceList.values.forEachIndexed { index, device ->
      Log.d(TAG, "Device #${index + 1}: ${device.deviceName}")
      Log.d(TAG, "  VendorId: 0x${device.vendorId.toString(16)}")
      Log.d(TAG, "  ProductId: 0x${device.productId.toString(16)}")
      Log.d(TAG, "  Class: ${device.deviceClass}")
      Log.d(TAG, "  Subclass: ${device.deviceSubclass}")
      Log.d(TAG, "  Protocol: ${device.deviceProtocol}")
      Log.d(TAG, "  InterfaceCount: ${device.interfaceCount}")
      Log.d(TAG, "  ManufacturerName: ${device.manufacturerName}")
      Log.d(TAG, "  ProductName: ${device.productName}")
      
      // Log interfaces
      for (i in 0 until device.interfaceCount) {
        val intf = device.getInterface(i)
        Log.d(TAG, "  Interface #$i:")
        Log.d(TAG, "    ID: ${intf.id}")
        Log.d(TAG, "    InterfaceClass: ${intf.interfaceClass}")
        Log.d(TAG, "    InterfaceSubclass: ${intf.interfaceSubclass}")
        Log.d(TAG, "    InterfaceProtocol: ${intf.interfaceProtocol}")
        Log.d(TAG, "    EndpointCount: ${intf.endpointCount}")
        
        // Log endpoints
        for (j in 0 until intf.endpointCount) {
          val endp = intf.getEndpoint(j)
          val direction = if (endp.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) "IN" else "OUT"
          val type = when (endp.type) {
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
            android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
            else -> "UNKNOWN"
          }
          Log.d(TAG, "      Endpoint #$j:")
          Log.d(TAG, "        Address: ${endp.address}")
          Log.d(TAG, "        Direction: $direction")
          Log.d(TAG, "        Type: $type")
          Log.d(TAG, "        MaxPacketSize: ${endp.maxPacketSize}")
          Log.d(TAG, "        Interval: ${endp.interval}")
        }
      }
    }
    
    Log.d(TAG, "========== END USB DEVICE DETAILS ==========")
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
    // HARD BLOCK: Refuse to connect to 0xBDA devices
    if (isBlockedConverter(device)) {
      Log.e(TAG, "BLOCKED: Refusing to connect to 0xBDA converter device: ${device.deviceName}")
      notifyListeners(ConnectionState.ERROR, "BLOCKED: Cannot connect to USB converter device (0xBDA). Please connect a MIDI device directly.")
      return
    }
    
    try {
      Log.d(TAG, "Attempting to connect to USB device: ${device.deviceName}")
      
      // Log detailed device information
      val vendorId = "0x${device.vendorId.toString(16).uppercase()}"
      val productId = "0x${device.productId.toString(16).uppercase()}"
      Log.d(TAG, "Device details: VID:$vendorId, PID:$productId, Class:${device.deviceClass}")
      
      // Check if we have permission
      if (!usbManager.hasPermission(device)) {
        Log.d(TAG, "No permission for device, requesting...")
        requestUsbPermission(device)
        return
      }
      
      // First try direct connection through USB Manager
      val success = tryDirectConnection(device)
      
      if (!success) {
        // If direct connection fails, try through MIDI Manager
        Log.d(TAG, "Direct connection failed, trying through MIDI Manager...")
        tryMidiManagerConnection(device)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting to USB device", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting to device: ${e.message}")
    }
  }
  
  /**
   * Try to establish a direct connection to a USB MIDI device
   */
  private fun tryDirectConnection(device: UsbDevice): Boolean {
    Log.d(TAG, "Attempting direct USB connection to device: ${device.deviceName}")
    
    try {
      // Clear existing connections first
      disconnect()
      
      // Get a connection to the device
      directUsbConnection = usbManager.openDevice(device)
      if (directUsbConnection == null) {
        Log.e(TAG, "Failed to open USB connection to device: ${device.deviceName}")
        notifyListeners(ConnectionState.ERROR, "Failed to open connection to device")
        return false
      }
      
      // Find a MIDI interface - look for class 1 (audio), subclass 3 (MIDI)
      var foundMidiInterface = false
      var fallbackInterface: android.hardware.usb.UsbInterface? = null
      
      Log.d(TAG, "Searching for MIDI interfaces among ${device.interfaceCount} interfaces")
      
      for (i in 0 until device.interfaceCount) {
        val usbInterface = device.getInterface(i)
        
        Log.d(TAG, "Interface #$i: Class=${usbInterface.interfaceClass}, Subclass=${usbInterface.interfaceSubclass}, Protocol=${usbInterface.interfaceProtocol}, Endpoints=${usbInterface.endpointCount}")
        
        // Look for standard MIDI interface (Audio class 1, MIDI subclass 3)
        if (usbInterface.interfaceClass == 1 && usbInterface.interfaceSubclass == 3) {
          directUsbInterface = usbInterface
          foundMidiInterface = true
          Log.d(TAG, "Found standard MIDI interface at index $i")
          break
        }
        
        // If we haven't found a fallback yet, and this is a vendor-specific interface with endpoints,
        // save it as a fallback option
        if (fallbackInterface == null && 
            usbInterface.interfaceClass == 255 && // Vendor specific
            usbInterface.endpointCount > 0) {
          fallbackInterface = usbInterface
          Log.d(TAG, "Found potential fallback vendor-specific interface at index $i")
        }
      }
      
      // If no standard MIDI interface was found, try using the fallback
      if (!foundMidiInterface && fallbackInterface != null) {
        Log.d(TAG, "No standard MIDI interface found, using vendor-specific interface as fallback")
        directUsbInterface = fallbackInterface
      } else if (!foundMidiInterface) {
        Log.e(TAG, "No suitable interface found for MIDI communication")
        notifyListeners(ConnectionState.ERROR, "Device does not have a MIDI interface")
        return false
      }
      
      // Try to claim the interface
      if (directUsbInterface != null && directUsbConnection != null) {
        val claimed = directUsbConnection!!.claimInterface(directUsbInterface, true)
        if (!claimed) {
          Log.e(TAG, "Failed to claim USB interface")
          notifyListeners(ConnectionState.ERROR, "Failed to claim USB interface")
          return false
        }
        Log.d(TAG, "Successfully claimed USB interface")
      } else {
        Log.e(TAG, "USB interface or connection is null")
        notifyListeners(ConnectionState.ERROR, "USB interface connection failed")
        return false
      }
      
      // Find input endpoints (direction == IN)
      val inputEndpoints = mutableListOf<android.hardware.usb.UsbEndpoint>()
      for (i in 0 until directUsbInterface!!.endpointCount) {
        val endpoint = directUsbInterface!!.getEndpoint(i)
        
        val direction = if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) "IN" else "OUT"
        val type = when (endpoint.type) {
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
          android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
          else -> "UNKNOWN"
        }
        
        Log.d(TAG, "Endpoint #$i: Direction=$direction, Type=$type, Address=0x${endpoint.address.toString(16)}")
        
        if (endpoint.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) {
          inputEndpoints.add(endpoint)
          Log.d(TAG, "Added input endpoint: $endpoint")
        }
      }
      
      if (inputEndpoints.isEmpty()) {
        Log.e(TAG, "No input endpoints found")
        notifyListeners(ConnectionState.ERROR, "No input endpoints found on the device")
        return false
      }
      
      // Start a thread to read from the first input endpoint
      val runningFlag = AtomicBoolean(true)
      directConnectionThread = Thread {
        Thread.currentThread().name = "USBMidiReaderThread"
        Log.d(TAG, "Starting USB MIDI reading thread")
        
        val endpoint = inputEndpoints[0]
        val buffer = ByteArray(endpoint.maxPacketSize)
        
        try {
          // Notify we're connected and ready to read
          notifyListeners(ConnectionState.CONNECTED, "Connected to ${device.deviceName}")
          
          while (runningFlag.get() && !Thread.currentThread().isInterrupted) {
            // Read from the endpoint with timeout
            val bytesRead = directUsbConnection?.bulkTransfer(endpoint, buffer, buffer.size, 100) ?: -1
            
            if (bytesRead > 0) {
              Log.d(TAG, "Read $bytesRead bytes from USB MIDI device")
              
              // Process the MIDI data
              val event = midiInputProcessor.processMidiData(buffer, 0, bytesRead, System.nanoTime())
              if (event != null) {
                Log.d(TAG, "Processed MIDI event: ${event.type}, Data1=${event.data1}, Data2=${event.data2}")
              } else {
                Log.d(TAG, "Unable to process MIDI data into an event")
              }
            } else if (bytesRead < 0) {
              // This happens normally due to timeout, only log on high verbosity
              Log.v(TAG, "Bulk transfer returned $bytesRead")
            }
            
            // Small sleep to prevent busy-waiting
            try {
              Thread.sleep(1)
            } catch (e: InterruptedException) {
              Log.d(TAG, "USB MIDI reading thread interrupted")
              break
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error reading from USB device", e)
          notifyListeners(ConnectionState.ERROR, "Error reading from USB device: ${e.message}")
        } finally {
          Log.d(TAG, "USB MIDI reading thread exiting")
          if (!runningFlag.get() && !Thread.currentThread().isInterrupted) {
            // If thread wasn't explicitly stopped but exited due to error,
            // disconnect properly
            Handler(Looper.getMainLooper()).post {
              disconnect()
            }
          }
        }
      }
      
      // Start the thread
      directConnectionThread?.start()
      
      Log.d(TAG, "Direct connection established successfully")
      return true
    } catch (e: Exception) {
      Log.e(TAG, "Error establishing direct connection", e)
      notifyListeners(ConnectionState.ERROR, "Error connecting to device: ${e.message}")
      return false
    }
  }

  private fun tryMidiManagerConnection(device: UsbDevice) {
    try {
      Log.d(TAG, "Trying MIDI Manager connection for device: ${device.deviceName}")
      
      // Check if this device has a MIDI interface
      var hasMidiInterface = false
      for (i in 0 until device.interfaceCount) {
        val usbInterface = device.getInterface(i)
        if (usbInterface.interfaceClass == 1 && usbInterface.interfaceSubclass == 3) {
          hasMidiInterface = true
          Log.d(TAG, "Found MIDI interface at index $i")
          break
        }
      }
      
      if (!hasMidiInterface) {
        Log.d(TAG, "Device does not have standard MIDI interfaces, but will try anyway")
      }
      
      // Try to find the device in the MIDI manager
      @Suppress("DEPRECATION")
      val devices = midiManager?.devices ?: emptyArray()
      
      Log.d(TAG, "Searching for USB device ${device.deviceName} in ${devices.size} MIDI devices")
      
      for (deviceInfo in devices) {
        if (deviceInfo.type == MidiDeviceInfo.TYPE_USB) {
          val properties = deviceInfo.properties
          val deviceId = properties.getInt(MidiDeviceInfo.PROPERTY_USB_DEVICE)
          Log.d(TAG, "Checking MIDI device with ID $deviceId against USB device ID ${device.deviceId}")
          
          if (deviceId == device.deviceId) {
            Log.d(TAG, "Found matching MIDI device for USB device ${device.deviceName}")
            connectToDevice(deviceInfo)
            return
          }
        }
      }
      
      Log.d(TAG, "USB device not recognized as MIDI device by Android MIDI service")
      notifyListeners(ConnectionState.ERROR, "USB device not recognized as MIDI device. Try selecting MIDI in USB settings.")
      
      // Register a callback to be notified when MIDI devices are added
      midiManager?.registerDeviceCallback(object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
          Log.d(TAG, "MIDI device added: ${device.properties}")
          handleMidiDevice(device)
        }
      }, Handler(Looper.getMainLooper()))
      
    } catch (e: Exception) {
      Log.e(TAG, "Error in MIDI Manager connection", e)
      notifyListeners(ConnectionState.ERROR, "Error in MIDI Manager connection: ${e.message}")
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
        
        // Connect to the device
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

  private fun requestUsbPermission(device: UsbDevice) {
    // HARD BLOCK: Refuse to request permission for 0xBDA devices
    if (isBlockedConverter(device)) {
      Log.e(TAG, "HARD BLOCK: Refusing to request permission for 0xBDA converter device: ${device.deviceName}")
      notifyListeners(ConnectionState.ERROR, "Cannot connect to USB converter device (0xBDA). Please connect a MIDI device directly.")
      return
    }
    
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
      Log.d(TAG, "Disconnecting USB/MIDI device")
      
      // First, stop the reading thread
      if (directConnectionThread != null && directConnectionThread!!.isAlive) {
        Log.d(TAG, "Interrupting USB reading thread")
        directConnectionThread?.interrupt()
        
        // Give the thread some time to clean up
        try {
          directConnectionThread?.join(500) // Wait up to 500ms for thread to exit
        } catch (e: InterruptedException) {
          Log.w(TAG, "Interrupted while waiting for USB thread to exit")
        }
      }
      directConnectionThread = null
      
      // Clean up MIDI connection
      try {
        if (currentInputPort != null) {
          Log.d(TAG, "Closing MIDI input port")
          currentInputPort?.close()
          currentInputPort = null
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error closing MIDI input port", e)
      }
      
      try {
        if (currentDevice != null) {
          Log.d(TAG, "Closing MIDI device")
          currentDevice?.close()
          currentDevice = null
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error closing MIDI device", e)
      }
      
      currentDeviceInfo = null
      
      // Clean up direct USB connection
      try {
        if (directUsbInterface != null && directUsbConnection != null) {
          Log.d(TAG, "Releasing USB interface")
          directUsbConnection?.releaseInterface(directUsbInterface)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error releasing USB interface", e)
      }
      
      try {
        if (directUsbConnection != null) {
          Log.d(TAG, "Closing USB connection")
          directUsbConnection?.close()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error closing USB connection", e)
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
    Log.d(TAG, "Cleaning up MIDI device manager resources")
    stopRetrying()
    disconnect()
    
    try {
      // Unregister USB receiver
      try {
        context.unregisterReceiver(usbReceiver)
        Log.d(TAG, "Unregistered USB receiver")
      } catch (e: Exception) {
        Log.e(TAG, "Error unregistering USB receiver", e)
      }
      
      // Clear all listeners
      listeners.clear()
      
      // Clear any cached data in the MIDI processor
      midiInputProcessor.cleanup()
      
      Log.d(TAG, "Cleanup completed")
    } catch (e: Exception) {
      Log.e(TAG, "Error during cleanup", e)
    }
  }
}
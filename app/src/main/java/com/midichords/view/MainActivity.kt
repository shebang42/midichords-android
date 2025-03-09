package com.midichords.view

import android.Manifest
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.midichords.databinding.ActivityMainBinding
import com.midichords.midi.ConnectionState
import com.midichords.midi.MidiEvent
import com.midichords.midi.MidiEventListener
import com.midichords.midi.MidiEventType
import com.midichords.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.LinkedList

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var viewModel: MainViewModel
  private lateinit var deviceAdapter: ArrayAdapter<String>
  private val deviceMap = mutableMapOf<String, UsbDevice>()

  // Debug variables
  private var debugMode = false
  private val maxLogEntries = 100
  private val logEntries = LinkedList<String>()
  
  // Controls visibility state
  private var controlsVisible = false

  companion object {
    private const val TAG = "MainActivity"
    private const val PERMISSION_REQUEST_CODE = 1001
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    setupUI()
    setupObservers()
    Log.d(TAG, "MainActivity created")

    // Initialize toggle button state
    binding.toggleControlsButton.isSelected = controlsVisible
    binding.controlsContainer.visibility = if (controlsVisible) View.VISIBLE else View.GONE
    binding.debugContainer.visibility = View.GONE
    
    // Ensure the toggle button is always on top
    binding.toggleControlsButton.bringToFront()

    // Move debug mode toggle to connection status long press
    binding.connectionStatus.setOnLongClickListener {
      toggleDebugMode()
      true
    }

    // Request permission to use MIDI devices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }
  }

  private fun setupUI() {
    // Initialize the device adapter
    deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
    binding.deviceList.adapter = deviceAdapter

    // Set up toggle controls button
    binding.toggleControlsButton.setOnClickListener {
      toggleControlsVisibility()
    }
    
    // Set up scan button
    binding.scanButton.setOnClickListener {
      Log.d(TAG, "Scan button clicked")
      scanForUsbDevices()
    }
    
    // Set up clear log button
    binding.btnClearLog.setOnClickListener {
      clearLog()
      Toast.makeText(this, "Debug log cleared", Toast.LENGTH_SHORT).show()
    }
    
    // Set up device list click listener
    binding.deviceList.setOnItemClickListener { _, _, position, _ ->
      val deviceName = deviceAdapter.getItem(position)
      deviceName?.let {
        val device = deviceMap[it]
        device?.let { usbDevice ->
          Log.d(TAG, "Selected device: $deviceName")
          displayDeviceDetails(usbDevice)
          
          // Add a dialog to ask if the user wants to connect to the device
          val builder = androidx.appcompat.app.AlertDialog.Builder(this)
          builder.setTitle("Connect to MIDI Device")
            .setMessage("Do you want to connect to this device?\n\n${usbDevice.deviceName}")
            .setPositiveButton("Connect") { _, _ ->
              viewModel.connectToDevice(usbDevice)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
              dialog.dismiss()
            }
            .show()
        }
      }
    }
    
    // Set up no chord message
    binding.chordDisplayView.setNoChordMessage("No Chord Detected")
  }
  
  private fun toggleControlsVisibility() {
    controlsVisible = !controlsVisible
    
    // Update the toggle button's selected state
    binding.toggleControlsButton.isSelected = controlsVisible
    
    // Ensure the toggle button is always on top
    binding.toggleControlsButton.bringToFront()
    
    // Show/hide the controls container with animation
    if (controlsVisible) {
      // Show controls with slide-up animation
      binding.controlsContainer.visibility = View.VISIBLE
      binding.controlsContainer.post {
        binding.controlsContainer.translationY = binding.controlsContainer.height.toFloat()
        binding.controlsContainer.animate()
          .translationY(0f)
          .setDuration(300)
          .start()
      }
      
      // If in debug mode, also show debug container
      if (debugMode) {
        binding.debugContainer.visibility = View.VISIBLE
        binding.debugContainer.post {
          binding.debugContainer.translationY = binding.debugContainer.height.toFloat()
          binding.debugContainer.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
        }
      }
    } else {
      // Hide controls with slide-down animation
      binding.controlsContainer.animate()
        .translationY(binding.controlsContainer.height.toFloat())
        .setDuration(300)
        .withEndAction {
          binding.controlsContainer.visibility = View.GONE
          binding.controlsContainer.translationY = 0f
        }
        .start()
      
      // Hide debug container if visible
      if (binding.debugContainer.visibility == View.VISIBLE) {
        binding.debugContainer.animate()
          .translationY(binding.debugContainer.height.toFloat())
          .setDuration(300)
          .withEndAction {
            binding.debugContainer.visibility = View.GONE
            binding.debugContainer.translationY = 0f
          }
          .start()
      }
    }
  }

  private fun setupObservers() {
    viewModel.connectionState.observe(this) { state ->
      Log.d(TAG, "Connection state changed to: $state")
      
      // Update the connection status text
      val statusText = when (state) {
        ConnectionState.CONNECTED -> "Status: Connected"
        ConnectionState.CONNECTING -> "Status: Connecting..."
        ConnectionState.DISCONNECTED -> "Status: Disconnected"
        ConnectionState.ERROR -> "Status: Error"
        else -> "Status: Unknown"
      }
      binding.connectionStatus.text = statusText
      
      // Update the UI elements based on connection state
      if (state == ConnectionState.CONNECTED) {
        binding.scanButton.isEnabled = false
        binding.deviceList.isEnabled = false
      } else {
        binding.scanButton.isEnabled = true
        binding.deviceList.isEnabled = true
      }
    }

    viewModel.connectionMessage.observe(this) { message ->
      Log.d(TAG, "Connection message: $message")
      message?.let {
        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
      }
    }

    viewModel.availableDevices.observe(this) { devices ->
      Log.d(TAG, "Available devices: ${devices.size}")
      updateDeviceList(devices)
    }
    
    // Observe MIDI events
    viewModel.lastMidiEvent.observe(this) { event ->
      // Update the MIDI event display
      val eventTypeStr = when (event.type) {
        MidiEventType.NOTE_ON -> "Note On"
        MidiEventType.NOTE_OFF -> "Note Off"
        MidiEventType.CONTROL_CHANGE -> "Control Change"
        else -> event.type.toString()
      }
      
      binding.midiEventInfo.text = "Event: $eventTypeStr"
      
      // For note events, show the note name
      if (event.type == MidiEventType.NOTE_ON || event.type == MidiEventType.NOTE_OFF) {
        val noteName = viewModel.getNoteNameFromNumber(event.data1)
        binding.midiNoteInfo.text = "Note: $noteName (${event.data1})"
        binding.midiVelocityInfo.text = "Velocity: ${event.data2}"
      }
      
      binding.midiChannelInfo.text = "Channel: ${event.channel + 1}" // Display 1-based channel number
    }
    
    // Observe active notes
    viewModel.activeNotes.observe(this) { notes ->
      binding.activeNotesInfo.text = "Active Notes: ${notes.size}"
      
      // If there are active notes, show the most recent one
      if (notes.isNotEmpty()) {
        val mostRecentNote = notes.maxByOrNull { it.timestamp }
        mostRecentNote?.let {
          val noteName = it.getNoteName()
          binding.midiNoteInfo.text = "Note: $noteName (${it.noteNumber})"
          binding.midiVelocityInfo.text = "Velocity: ${it.velocity}"
          binding.midiChannelInfo.text = "Channel: ${it.channel + 1}" // Display 1-based channel number
        }
      }
    }
    
    // Observe current chord
    viewModel.currentChord.observe(this) { chord ->
      Log.d(TAG, "Current chord updated: ${chord?.getName() ?: "None"}")
      
      // Update ChordDisplayView
      binding.chordDisplayView.setChord(chord)
    }
  }
  
  private fun scanForUsbDevices() {
    // Get USB devices directly from UsbManager
    val usbManager = getSystemService(USB_SERVICE) as UsbManager
    val allDevices = usbManager.deviceList
    
    // Log ALL devices for debugging
    Log.d(TAG, "===== ALL USB DEVICES =====")
    allDevices.forEach { (name, device) ->
      val vendorId = "0x${device.vendorId.toString(16).uppercase()}"
      val productId = "0x${device.productId.toString(16).uppercase()}"
      Log.d(TAG, "Device: $name, VID: $vendorId, PID: $productId, Class: ${device.deviceClass}, Interfaces: ${device.interfaceCount}")
    }
    Log.d(TAG, "==========================")
    
    if (allDevices.isEmpty()) {
      Toast.makeText(this, "No USB devices found", Toast.LENGTH_SHORT).show()
      binding.deviceDetails.text = "No USB devices found"
      return
    }
    
    // Filter out the 0xBDA converter device
    val filteredDevices = allDevices.filter { (_, device) -> device.vendorId != 0x0BDA }
    
    if (filteredDevices.isEmpty()) {
      Toast.makeText(this, "Only found USB converter devices (0xBDA). Please connect a MIDI device directly.", Toast.LENGTH_LONG).show()
      
      // Show detailed information about the converter device for debugging
      val details = StringBuilder("Only found USB converter devices:\n\n")
      
      allDevices.forEach { (name, device) ->
        val vendorId = "0x${device.vendorId.toString(16).uppercase()}"
        val productId = "0x${device.productId.toString(16).uppercase()}"
        
        details.append("Device: $name\n")
        details.append("  Vendor ID: $vendorId\n")
        details.append("  Product ID: $productId\n")
        details.append("  Class: ${device.deviceClass}\n")
        details.append("  Interfaces: ${device.interfaceCount}\n\n")
        
        // Log interfaces
        for (i in 0 until device.interfaceCount) {
          val usbInterface = device.getInterface(i)
          details.append("  Interface $i: Class ${usbInterface.interfaceClass}, Subclass ${usbInterface.interfaceSubclass}\n")
        }
      }
      
      binding.deviceDetails.text = details.toString()
      return
    }
    
    // Clear previous devices
    deviceMap.clear()
    val deviceNames = mutableListOf<String>()
    
    // Add filtered USB devices to the list
    filteredDevices.forEach { (name, device) ->
      // Create a more descriptive name for the device
      val vendorId = "0x${device.vendorId.toString(16).uppercase()}"
      val productId = "0x${device.productId.toString(16).uppercase()}"
      val displayName = "$name (VID:$vendorId, PID:$productId)"
      
      deviceMap[displayName] = device
      deviceNames.add(displayName)
      Log.d(TAG, "Found non-converter USB device: $displayName (ID: ${device.deviceId})")
    }
    
    // Update the adapter
    deviceAdapter.clear()
    deviceAdapter.addAll(deviceNames)
    deviceAdapter.notifyDataSetChanged()
    
    // Show a message
    Toast.makeText(this, "Found ${filteredDevices.size} USB devices (excluding 0xBDA converters)", Toast.LENGTH_SHORT).show()
    
    // If we have devices, handle connection automatically if only one device, else prompt the user
    if (deviceNames.isNotEmpty()) {
      if (deviceNames.size == 1) {
        val device = deviceMap[deviceNames[0]]
        device?.let {
          displayDeviceDetails(it)
          viewModel.connectToDevice(it)
          Toast.makeText(this, "Automatically connected to ${it.deviceName}", Toast.LENGTH_SHORT).show()
        }
      } else {
        val firstDevice = deviceMap[deviceNames[0]]
        firstDevice?.let {
          displayDeviceDetails(it)

          // Add a dialog to ask if the user wants to connect to the device
          val builder = androidx.appcompat.app.AlertDialog.Builder(this)
          builder.setTitle("Connect to MIDI Device")
            .setMessage("Do you want to connect to this device?\n\n${it.deviceName}\nVendor ID: 0x${it.vendorId.toString(16).uppercase()}\nProduct ID: 0x${it.productId.toString(16).uppercase()}")
            .setPositiveButton("Connect") { _, _ ->
              viewModel.connectToDevice(it)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
              dialog.dismiss()
            }
            .show()
        }
      }
    }
  }
  
  private fun displayAllDevices(devices: Map<String, UsbDevice>) {
    // For debugging - show all devices including converters
    val details = StringBuilder("All connected USB devices (including converters):\n\n")
    
    devices.forEach { (name, device) ->
      val vendorId = "0x${device.vendorId.toString(16).uppercase()}"
      val productId = "0x${device.productId.toString(16).uppercase()}"
      
      details.append("Device: $name\n")
      details.append("  Vendor ID: $vendorId\n")
      details.append("  Product ID: $productId\n")
      details.append("  Class: ${device.deviceClass}\n")
      details.append("  Interfaces: ${device.interfaceCount}\n\n")
    }
    
    binding.deviceDetails.text = details.toString()
  }
  
  private fun updateDeviceList(devices: List<UsbDevice>) {
    // Clear previous devices
    deviceMap.clear()
    val deviceNames = mutableListOf<String>()
    
    // Add all devices to the list
    devices.forEach { device ->
      val name = device.deviceName
      deviceMap[name] = device
      deviceNames.add(name)
    }
    
    // Update the adapter
    deviceAdapter.clear()
    deviceAdapter.addAll(deviceNames)
    deviceAdapter.notifyDataSetChanged()
    
    // If we have devices, show details for the first one
    if (deviceNames.isNotEmpty()) {
      val firstDevice = deviceMap[deviceNames[0]]
      firstDevice?.let {
        displayDeviceDetails(it)
      }
    } else {
      binding.deviceDetails.text = "No devices available"
    }
  }
  
  private fun displayDeviceDetails(device: UsbDevice) {
    val details = StringBuilder()
    details.append("Name: ${device.deviceName}\n")
    details.append("ID: ${device.deviceId}\n")
    details.append("Vendor ID: ${device.vendorId} (0x${device.vendorId.toString(16).uppercase()})\n")
    details.append("Product ID: ${device.productId} (0x${device.productId.toString(16).uppercase()})\n")
    details.append("Class: ${device.deviceClass}\n")
    details.append("Subclass: ${device.deviceSubclass}\n")
    details.append("Interfaces: ${device.interfaceCount}\n")
    
    // Check if this device has a MIDI interface
    var hasMidiInterface = false
    var potentialMidiInterface = false
    
    for (i in 0 until device.interfaceCount) {
      val usbInterface = device.getInterface(i)
      val interfaceClass = usbInterface.interfaceClass
      val interfaceSubclass = usbInterface.interfaceSubclass
      
      if (interfaceClass == 1 && interfaceSubclass == 3) {
        hasMidiInterface = true
        details.append("Interface $i: MIDI (Class 1, Subclass 3)\n")
      } else if (interfaceClass == 2 && interfaceSubclass == 6) {
        potentialMidiInterface = true
        details.append("Interface $i: Potential MIDI (Class 2, Subclass 6)\n")
      } else if (interfaceClass == 255) {
        potentialMidiInterface = true
        details.append("Interface $i: Vendor-specific (Class 255, Subclass $interfaceSubclass)\n")
      } else {
        details.append("Interface $i: Class $interfaceClass, Subclass $interfaceSubclass\n")
      }
    }
    
    if (hasMidiInterface) {
      details.append("This device has a standard MIDI interface")
    } else if (potentialMidiInterface) {
      details.append("This device has a potential MIDI interface (non-standard)")
    } else {
      details.append("This device does NOT have a standard MIDI interface")
    }
    
    binding.deviceDetails.text = details.toString()
  }

  /**
   * Shows instructions for setting USB mode to MIDI
   */
  private fun showUsbModeInstructions() {
    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
    builder.setTitle("USB Mode Settings")
    builder.setMessage(
      "Your device is showing 'Couldn't switch' for the 'This device' option. This is a common limitation with some Android devices.\n\n" +
      "To use MIDI devices with this app:\n\n" +
      "1. Make sure your MIDI device is connected to your Android device\n\n" +
      "2. Select 'MIDI' under 'Use USB for' in your Android settings\n\n" +
      "3. If you can't select 'MIDI', try these workarounds:\n" +
      "   - Disconnect and reconnect your MIDI device\n" +
      "   - Try a different USB port if available\n" +
      "   - Restart your Android device with the MIDI device connected\n" +
      "   - Some devices only work in 'Connected device' mode\n\n" +
      "The app will automatically try to connect to your MIDI device even if you can't change the USB mode."
    )
    builder.setPositiveButton("Open Android Settings") { _, _ ->
      openUsbSettings()
    }
    builder.setNegativeButton("Cancel", null)
    builder.show()
  }

  /**
   * Opens Android USB settings
   */
  private fun openUsbSettings() {
    try {
      // Try to open system settings since there's no direct USB settings action
      val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
      startActivity(intent)
      Toast.makeText(this, "Please navigate to 'Connected devices' or 'USB' settings", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to open settings", e)
      Toast.makeText(this, "Could not open settings. Please open them manually.", Toast.LENGTH_LONG).show()
    }
  }

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "MainActivity resumed, scanning USB devices")
    scanForUsbDevices()
    binding.root.postDelayed({ viewModel.refreshAvailableDevices() }, 500)
  }

  private fun toggleDebugMode() {
    debugMode = !debugMode
    
    if (debugMode) {
      // Show debug UI only if controls are visible
      if (controlsVisible) {
        binding.debugContainer.visibility = View.VISIBLE
      }
      Toast.makeText(this, "Debug mode enabled", Toast.LENGTH_SHORT).show()
      
      // Add debug MIDI event listener
      viewModel.addMidiEventListener(object : MidiEventListener {
        override fun onMidiEvent(event: MidiEvent) {
          // Log the event to our debug display
          addLogEntry("MIDI: ${event.type} ch:${event.channel} d1:${event.data1} d2:${event.data2}")
        }
      })
    } else {
      // Hide debug UI
      binding.debugContainer.visibility = View.GONE
      Toast.makeText(this, "Debug mode disabled", Toast.LENGTH_SHORT).show()
    }
  }
  
  private fun addLogEntry(entry: String) {
    // Add timestamp
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
    val timestampedEntry = "$timestamp - $entry"
    
    // Add to our collection with a max size limit
    synchronized(logEntries) {
      logEntries.addFirst(timestampedEntry)
      while (logEntries.size > maxLogEntries) {
        logEntries.removeLast()
      }
    }
    
    // Update the UI on the main thread
    runOnUiThread {
      updateLogDisplay()
    }
  }
  
  private fun updateLogDisplay() {
    val sb = StringBuilder()
    synchronized(logEntries) {
      for (entry in logEntries) {
        sb.append(entry).append("\n")
      }
    }
    binding.tvDebugLog.text = sb.toString()
  }
  
  private fun clearLog() {
    synchronized(logEntries) {
      logEntries.clear()
    }
    updateLogDisplay()
  }
} 
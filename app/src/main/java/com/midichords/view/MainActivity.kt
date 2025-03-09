package com.midichords.view

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.midichords.databinding.ActivityMainBinding
import com.midichords.midi.ConnectionState
import com.midichords.midi.MidiEventType
import com.midichords.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var viewModel: MainViewModel
  private lateinit var deviceAdapter: ArrayAdapter<String>
  private val deviceMap = mutableMapOf<String, UsbDevice>()

  companion object {
    private const val TAG = "MainActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    setupUI()
    setupObservers()
    Log.d(TAG, "MainActivity created")
  }

  private fun setupUI() {
    // Initialize the device adapter
    deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
    binding.deviceList.adapter = deviceAdapter

    // Set up scan button
    binding.scanButton.setOnClickListener {
      Log.d(TAG, "Scan button clicked")
      scanForUsbDevices()
    }
    
    // Set up test MIDI button
    binding.testMidiButton.setOnClickListener {
      Log.d(TAG, "Test MIDI button clicked")
      viewModel.sendTestMidiMessage()
      Toast.makeText(this, "Sending test MIDI message (C4 note)", Toast.LENGTH_SHORT).show()
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
  }

  private fun setupObservers() {
    viewModel.connectionState.observe(this) { state ->
      Log.d(TAG, "Connection state changed to: $state")
      binding.connectionStatus.text = state.toString()
      
      // If we're connected, update the UI
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
  }
  
  private fun scanForUsbDevices() {
    // Get USB devices directly from UsbManager
    val usbManager = getSystemService(USB_SERVICE) as UsbManager
    val deviceList = usbManager.deviceList
    
    if (deviceList.isEmpty()) {
      Toast.makeText(this, "No USB devices found", Toast.LENGTH_SHORT).show()
      binding.deviceDetails.text = "No USB devices found"
      return
    }
    
    // Clear previous devices
    deviceMap.clear()
    val deviceNames = mutableListOf<String>()
    
    // Add all USB devices to the list
    deviceList.forEach { (name, device) ->
      // Create a more descriptive name for the device
      val vendorId = "0x${device.vendorId.toString(16).uppercase()}"
      val productId = "0x${device.productId.toString(16).uppercase()}"
      val displayName = "$name (VID:$vendorId, PID:$productId)"
      
      deviceMap[displayName] = device
      deviceNames.add(displayName)
      Log.d(TAG, "Found USB device: $displayName (ID: ${device.deviceId})")
    }
    
    // Update the adapter
    deviceAdapter.clear()
    deviceAdapter.addAll(deviceNames)
    deviceAdapter.notifyDataSetChanged()
    
    // Show a message
    Toast.makeText(this, "Found ${deviceList.size} USB devices", Toast.LENGTH_SHORT).show()
    
    // If we have devices, show details for the first one
    if (deviceNames.isNotEmpty()) {
      val firstDevice = deviceMap[deviceNames[0]]
      firstDevice?.let {
        displayDeviceDetails(it)
      }
    }
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

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "MainActivity resumed, refreshing devices")
    viewModel.refreshAvailableDevices()
  }
} 
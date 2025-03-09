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
import com.midichords.viewmodel.ConnectionState
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

    // Set up device list click listener
    binding.deviceList.setOnItemClickListener { _, _, position, _ ->
      val deviceName = deviceAdapter.getItem(position)
      deviceName?.let {
        val device = deviceMap[it]
        device?.let { usbDevice ->
          Log.d(TAG, "Selected device: $deviceName")
          displayDeviceDetails(usbDevice)
          viewModel.connectToDevice(usbDevice)
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
      deviceMap[name] = device
      deviceNames.add(name)
      Log.d(TAG, "Found USB device: $name (ID: ${device.deviceId})")
    }
    
    // Update the adapter
    deviceAdapter.clear()
    deviceAdapter.addAll(deviceNames)
    deviceAdapter.notifyDataSetChanged()
    
    // Show a message
    Toast.makeText(this, "Found ${deviceList.size} USB devices", Toast.LENGTH_SHORT).show()
    
    // If we have devices, try to connect to the first one
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
    details.append("Vendor ID: ${device.vendorId}\n")
    details.append("Product ID: ${device.productId}\n")
    details.append("Class: ${device.deviceClass}\n")
    details.append("Subclass: ${device.deviceSubclass}\n")
    details.append("Interfaces: ${device.interfaceCount}\n")
    
    // Check if this device has a MIDI interface
    var hasMidiInterface = false
    for (i in 0 until device.interfaceCount) {
      val usbInterface = device.getInterface(i)
      if (usbInterface.interfaceClass == 1 && usbInterface.interfaceSubclass == 3) {
        hasMidiInterface = true
        details.append("Interface $i: MIDI\n")
      } else {
        details.append("Interface $i: Class ${usbInterface.interfaceClass}, Subclass ${usbInterface.interfaceSubclass}\n")
      }
    }
    
    if (hasMidiInterface) {
      details.append("This device has a MIDI interface")
    } else {
      details.append("This device does NOT have a MIDI interface")
    }
    
    binding.deviceDetails.text = details.toString()
  }

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "MainActivity resumed, refreshing devices")
    viewModel.refreshAvailableDevices()
  }
} 
package com.midichords.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.midichords.databinding.ActivityMainBinding
import com.midichords.viewmodel.ConnectionState
import com.midichords.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var viewModel: MainViewModel

  companion object {
    private const val TAG = "MainActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    setupObservers()
    Log.d(TAG, "MainActivity created")
  }

  private fun setupObservers() {
    viewModel.connectionState.observe(this) { state ->
      Log.d(TAG, "Connection state changed to: $state")
      binding.connectionStatus.text = state.toString()
    }

    viewModel.connectionMessage.observe(this) { message ->
      Log.d(TAG, "Connection message: $message")
      message?.let {
        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
      }
    }

    viewModel.availableDevices.observe(this) { devices ->
      Log.d(TAG, "Available devices: ${devices.size}")
      // TODO: Show available devices in UI
    }
  }

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "MainActivity resumed, refreshing devices")
    viewModel.refreshAvailableDevices()
  }
} 
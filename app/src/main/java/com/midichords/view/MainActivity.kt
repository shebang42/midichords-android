package com.midichords.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.midichords.databinding.ActivityMainBinding
import com.midichords.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var viewModel: MainViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    setupObservers()
  }

  private fun setupObservers() {
    // TODO: Implement observers for connection state, active notes, and current chord
  }
} 
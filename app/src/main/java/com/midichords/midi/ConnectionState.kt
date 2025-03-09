package com.midichords.midi

/**
 * Represents the connection state of a MIDI device.
 */
enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
} 
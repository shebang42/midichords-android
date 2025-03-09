package com.midichords.midi

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import com.midichords.midi.ConnectionState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MidiDeviceManagerImplTest {

  private lateinit var context: Context
  private lateinit var usbManager: UsbManager
  private lateinit var midiManager: MidiManager
  private lateinit var midiDeviceManager: MidiDeviceManagerImpl
  private lateinit var listener: ConnectionStateListener
  private lateinit var mockDevice: UsbDevice
  private lateinit var mockInterface: UsbInterface

  @Before
  fun setup() {
    context = mock()
    usbManager = mock()
    midiManager = mock()
    listener = mock()
    mockDevice = mock()
    mockInterface = mock()

    whenever(context.getSystemService(Context.USB_SERVICE)).thenReturn(usbManager)
    whenever(context.getSystemService(Context.MIDI_SERVICE)).thenReturn(midiManager)
    whenever(mockDevice.interfaceCount).thenReturn(1)
    whenever(mockDevice.getInterface(0)).thenReturn(mockInterface)

    midiDeviceManager = MidiDeviceManagerImpl(context, usbManager, midiManager)
    midiDeviceManager.registerListener(listener)
  }

  @Test
  fun `test registerListener adds listener`() {
    val newListener: ConnectionStateListener = mock()
    midiDeviceManager.registerListener(newListener)

    // Trigger a state change to verify both listeners are notified
    midiDeviceManager.disconnect()
    
    verify(listener).onConnectionStateChanged(eq(ConnectionState.DISCONNECTED), isNull())
    verify(newListener).onConnectionStateChanged(eq(ConnectionState.DISCONNECTED), isNull())
  }

  @Test
  fun `test unregisterListener removes listener`() {
    midiDeviceManager.unregisterListener(listener)
    
    // Trigger a state change to verify listener is not notified
    midiDeviceManager.disconnect()
    
    verify(listener, never()).onConnectionStateChanged(any(), any())
  }

  @Test
  fun `test requestPermission when already has permission`() {
    whenever(usbManager.hasPermission(mockDevice)).thenReturn(true)
    
    midiDeviceManager.requestPermission(mockDevice)
    
    verify(listener).onConnectionStateChanged(eq(ConnectionState.CONNECTING), isNull())
  }

  @Test
  fun `test requestPermission when needs permission`() {
    whenever(usbManager.hasPermission(mockDevice)).thenReturn(false)
    
    midiDeviceManager.requestPermission(mockDevice)
    
    verify(listener).onConnectionStateChanged(
      eq(ConnectionState.CONNECTING),
      eq("Requesting permission for device")
    )
  }

  @Test
  fun `test getAvailableDevices filters audio class devices`() {
    val devices = mapOf("device1" to mockDevice)
    whenever(usbManager.deviceList).thenReturn(devices)
    whenever(mockInterface.interfaceClass).thenReturn(UsbConstants.USB_CLASS_AUDIO)
    
    val availableDevices = midiDeviceManager.getAvailableDevices()
    
    assert(availableDevices.contains(mockDevice))
  }

  @Test
  fun `test getAvailableDevices filters non-audio devices`() {
    val devices = mapOf("device1" to mockDevice)
    whenever(usbManager.deviceList).thenReturn(devices)
    whenever(mockInterface.interfaceClass).thenReturn(UsbConstants.USB_CLASS_HID)
    
    val availableDevices = midiDeviceManager.getAvailableDevices()
    
    assert(!availableDevices.contains(mockDevice))
  }

  @Test
  fun `test disconnect closes device and notifies listeners`() {
    val midiDevice: MidiDevice = mock()
    whenever(midiManager.openDevice(any(), any(), any())).thenAnswer {
      (it.arguments[1] as (MidiDevice?) -> Unit).invoke(midiDevice)
    }

    midiDeviceManager.connect(mockDevice)
    midiDeviceManager.disconnect()
    
    verify(listener).onConnectionStateChanged(eq(ConnectionState.DISCONNECTED), isNull())
  }

  @Test
  fun `test dispose cleans up resources`() {
    midiDeviceManager.dispose()
    
    verify(context).unregisterReceiver(any())
  }
} 
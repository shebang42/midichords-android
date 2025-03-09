package com.midichords.midi

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MidiInputProcessorTest {
  
  @Test
  fun `test note on message parsing`() {
    val processor = MidiInputProcessorImpl()
    val listener = mock<MidiEventListener>()
    processor.registerListener(listener)

    val data = byteArrayOf(
      0x90.toByte(), // Note On, channel 0
      60.toByte(),   // Middle C
      100.toByte()   // Velocity
    )

    val event = processor.processMidiData(data, 0, 3, System.nanoTime())
    assert(event != null)
    event?.let {
      assert(it.type == MidiEventType.NOTE_ON)
      assert(it.channel == 0)
      assert(it.data1 == 60)
      assert(it.data2 == 100)
      assert(it.isNoteOn())
    }

    verify(listener).onMidiEvent(event!!)
  }

  @Test
  fun `test note off message parsing`() {
    val processor = MidiInputProcessorImpl()
    val listener = mock<MidiEventListener>()
    processor.registerListener(listener)

    val data = byteArrayOf(
      0x80.toByte(), // Note Off, channel 0
      60.toByte(),   // Middle C
      0.toByte()     // Velocity
    )

    val event = processor.processMidiData(data, 0, 3, System.nanoTime())
    assert(event != null)
    event?.let {
      assert(it.type == MidiEventType.NOTE_OFF)
      assert(it.channel == 0)
      assert(it.data1 == 60)
      assert(it.data2 == 0)
      assert(it.isNoteOff())
    }

    verify(listener).onMidiEvent(event!!)
  }

  @Test
  fun `test note on with zero velocity is treated as note off`() {
    val processor = MidiInputProcessorImpl()
    val listener = mock<MidiEventListener>()
    processor.registerListener(listener)

    val data = byteArrayOf(
      0x90.toByte(), // Note On, channel 0
      60.toByte(),   // Middle C
      0.toByte()     // Zero velocity = Note Off
    )

    val event = processor.processMidiData(data, 0, 3, System.nanoTime())
    assert(event != null)
    event?.let {
      assert(it.type == MidiEventType.NOTE_ON)
      assert(it.channel == 0)
      assert(it.data1 == 60)
      assert(it.data2 == 0)
      assert(it.isNoteOff())
    }

    verify(listener).onMidiEvent(event!!)
  }

  @Test
  fun `test sustain pedal message parsing`() {
    val processor = MidiInputProcessorImpl()
    val listener = mock<MidiEventListener>()
    processor.registerListener(listener)

    val data = byteArrayOf(
      0xB0.toByte(), // Control Change, channel 0
      64.toByte(),   // Sustain pedal controller
      127.toByte()   // Value (on)
    )

    val event = processor.processMidiData(data, 0, 3, System.nanoTime())
    assert(event != null)
    event?.let {
      assert(it.type == MidiEventType.CONTROL_CHANGE)
      assert(it.channel == 0)
      assert(it.data1 == 64)
      assert(it.data2 == 127)
      assert(it.isSustainPedal())
      assert(it.isSustainOn())
    }

    verify(listener).onMidiEvent(event!!)
  }

  @Test
  fun `test invalid message handling`() {
    val processor = MidiInputProcessorImpl()
    val listener = mock<MidiEventListener>()
    processor.registerListener(listener)

    // Test with incomplete message
    val data = byteArrayOf(
      0x90.toByte(), // Note On, channel 0
      60.toByte()    // Middle C, missing velocity
    )

    val event = processor.processMidiData(data, 0, 2, System.nanoTime())
    assert(event == null)
  }

  @Test
  fun `test listener management`() {
    val processor = MidiInputProcessorImpl()
    val listener1 = mock<MidiEventListener>()
    val listener2 = mock<MidiEventListener>()

    processor.registerListener(listener1)
    processor.registerListener(listener2)

    val data = byteArrayOf(
      0x90.toByte(),
      60.toByte(),
      100.toByte()
    )

    val event = processor.processMidiData(data, 0, 3, System.nanoTime())
    assert(event != null)

    verify(listener1).onMidiEvent(event!!)
    verify(listener2).onMidiEvent(event)

    processor.unregisterListener(listener1)
    val event2 = processor.processMidiData(data, 0, 3, System.nanoTime())
    verify(listener2).onMidiEvent(event2!!)
  }
} 
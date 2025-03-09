# Android MIDI Note and Chord Display App
## Technical Specification Document

### 1. Overview

This document provides comprehensive specifications for an Android application designed to process MIDI input from USB keyboards, identify notes and chords, and display them in musical notation and text format in real-time. The application targets musicians who want to visualize their playing and identify chords.

### 2. Core Requirements

#### 2.1 Functional Requirements

- Connect to and receive MIDI data from USB MIDI keyboards
- Display played notes on a musical staff in standard notation
- Identify and display chord names when multiple notes are played simultaneously
- Support for sustained notes when the sustain pedal is active
- Real-time display with consistent refresh rate (10 fps)
- Operate in both portrait and landscape orientations
- Display both treble and bass clefs for full range coverage
- Handle USB connection/disconnection events gracefully
- Support Android 8.0 (API level 26) and higher

#### 2.2 Non-Functional Requirements

- Maximum latency between key press and display: 100ms
- Memory usage under 150MB in normal operation
- Battery usage optimization for extended play sessions
- Graceful degradation when system resources are constrained
- Clean handling of edge cases (connection loss, malformed MIDI data)

### 3. Technical Architecture

#### 3.1 High-Level Architecture

The application follows the MVVM (Model-View-ViewModel) architectural pattern with the following components:

```
┌────────────────┐    ┌────────────────┐    ┌────────────────┐
│   USB/MIDI     │    │  Application   │    │      UI        │
│   Subsystem    │━━━▶│     Logic      │━━━▶│   Components   │
└────────────────┘    └────────────────┘    └────────────────┘
```

1. **USB/MIDI Subsystem**: Handles USB device connection, MIDI protocol parsing, and event generation
2. **Application Logic**: Processes MIDI events, identifies notes and chords, manages application state
3. **UI Components**: Renders staff notation, chord names, and handles user interaction

#### 3.2 Component Breakdown

##### 3.2.1 USB/MIDI Subsystem

- `MidiDeviceManager`: Handles USB device detection, connection, permission requests
- `MidiInputProcessor`: Parses raw MIDI data into structured events
- `MidiEventDispatcher`: Distributes MIDI events to registered listeners

##### 3.2.2 Application Logic

- `NoteProcessor`: Tracks currently active notes and their states
- `ChordIdentifier`: Analyzes active notes to identify chord names
- `SessionManager`: Manages the overall state of the current playing session
- `PreferenceManager`: Handles application settings (minimal in initial version)

##### 3.2.3 UI Components

- `StaffView`: Custom view for rendering musical staff notation
- `ChordDisplayView`: Displays identified chord names
- `ConnectionStatusView`: Shows USB/MIDI connection status
- `MainActivityViewModel`: Mediates between model and view layers

### 4. Data Flow and State Management

#### 4.1 MIDI Data Flow

1. USB MIDI device sends data to Android device
2. `MidiDeviceManager` receives raw MIDI data
3. `MidiInputProcessor` parses data into structured MIDI events
4. `MidiEventDispatcher` sends events to `NoteProcessor`
5. `NoteProcessor` updates internal state of active notes
6. `ChordIdentifier` analyzes active notes to determine chord names
7. `MainActivityViewModel` receives state updates
8. UI views observe ViewModel and update display accordingly

#### 4.2 State Management

The app maintains the following states:

- **Connection State**: Connected/Disconnected/Requesting Permission
- **Active Notes**: Collection of currently pressed or sustained notes
- **Identified Chord**: Current chord name based on active notes
- **Sustain Pedal State**: On/Off
- **UI State**: Loading/Ready/Error

State changes are propagated through LiveData objects in the ViewModel.

### 5. Detailed Technical Specifications

#### 5.1 MIDI Processing

##### 5.1.1 MIDI Message Handling

The app must handle the following MIDI message types:

- **Note On** (0x90): Key pressed
- **Note Off** (0x80): Key released
- **Control Change** (0xB0): For sustain pedal (CC #64)

```kotlin
// Example structure for a MIDI event
data class MidiEvent(
    val type: MidiEventType,
    val channel: Int,
    val data1: Int,  // Note number for NoteOn/NoteOff, controller number for ControlChange
    val data2: Int,  // Velocity for NoteOn/NoteOff, value for ControlChange
    val timestamp: Long
)

enum class MidiEventType {
    NOTE_ON,
    NOTE_OFF,
    CONTROL_CHANGE,
    PROGRAM_CHANGE,
    PITCH_BEND,
    OTHER
}
```

##### 5.1.2 Note Tracking

The `NoteProcessor` must:

- Track notes that are currently pressed
- Track notes that are sustained via pedal
- Associate timestamp with each note event
- Handle edge cases (e.g., Note Off without corresponding Note On)

```kotlin
// Example note tracking structure
data class ActiveNote(
    val noteNumber: Int,
    val velocity: Int,
    val startTime: Long,
    val channel: Int,
    val isSustained: Boolean = false
)
```

#### 5.2 Chord Identification

The `ChordIdentifier` should implement the following algorithm:

1. Collect all active notes (pressed or sustained)
2. Normalize notes to a single octave to identify chord type
3. Determine the root note
4. Match the note pattern against known chord types
5. Generate chord name in standard notation (e.g., "C", "Dm", "G7")

Support the following chord types in initial version:
- Major (e.g., C, F, G)
- Minor (e.g., Cm, Fm, Gm)
- Dominant 7th (e.g., C7, F7, G7)
- Major 7th (e.g., Cmaj7, Fmaj7, Gmaj7)
- Minor 7th (e.g., Cm7, Fm7, Gm7)
- Diminished (e.g., Cdim, Fdim, Gdim)
- Augmented (e.g., Caug, Faug, Gaug)
- Suspended 2nd and 4th (e.g., Csus2, Fsus4)

#### 5.3 Musical Notation Rendering

The `StaffView` should:

- Display standard 5-line staff for both treble and bass clefs
- Render quarter notes for all active notes regardless of actual duration
- Support accidentals (sharps and flats)
- Handle ledger lines for notes outside the staff
- Implement proper musical spacing based on standard notation rules
- Refresh at 10 frames per second (100ms intervals)

The staff should display notes in the range from C2 (two octaves below middle C) to C6 (two octaves above middle C).

#### 5.4 UI Layout

##### 5.4.1 Portrait Orientation

```
┌───────────────────────┐
│  Connection Status    │
├───────────────────────┤
│                       │
│                       │
│    Musical Staff      │
│    (Treble & Bass)    │
│                       │
│                       │
├───────────────────────┤
│                       │
│    Chord Display      │
│                       │
└───────────────────────┘
```

##### 5.4.2 Landscape Orientation

```
┌───────────────────┬───────────────────┐
│  Connection       │                   │
│  Status           │                   │
├───────────────────┤   Chord Display   │
│                   │                   │
│  Musical Staff    │                   │
│  (Treble & Bass)  │                   │
│                   │                   │
└───────────────────┴───────────────────┘
```

#### 5.5 USB and Permissions

The app requires:

- USB Host Feature
- MIDI feature support
- Runtime permission handling for USB device access

Implement proper permission request workflows with clear user guidance.

### 6. Error Handling Strategy

#### 6.1 Error Categories

1. **Connection Errors**
   - Device disconnection
   - Permission denied
   - Unsupported MIDI device

2. **MIDI Data Errors**
   - Malformed MIDI messages
   - Unexpected message sequences
   - Data corruption

3. **Application Errors**
   - Resource limitations
   - View rendering failures
   - Unexpected crashes

#### 6.2 Error Handling Approach

For all errors:
1. Log detailed diagnostic information
2. Present user-friendly error messages
3. Attempt recovery when possible
4. Fail gracefully when recovery is not possible

Example error handling patterns:

```kotlin
// Example connection error handling
try {
    midiDeviceManager.connectDevice(device)
} catch (e: DeviceConnectionException) {
    logError("Failed to connect to MIDI device", e)
    when (e) {
        is PermissionDeniedException -> showPermissionRequiredMessage()
        is DeviceDisconnectedException -> showDeviceDisconnectedMessage()
        is UnsupportedDeviceException -> showUnsupportedDeviceMessage()
        else -> showGenericConnectionErrorMessage()
    }
}

// Example MIDI data error handling
try {
    val event = midiInputProcessor.processMessage(rawData)
    midiEventDispatcher.dispatchEvent(event)
} catch (e: MidiDataException) {
    logWarning("MIDI data error", e)
    // Ignore malformed data but continue processing
}
```

### 7. Performance Considerations

#### 7.1 Threading Model

1. **Main Thread**: UI rendering and user interaction
2. **MIDI Processing Thread**: Dedicated background thread for MIDI data processing
3. **Rendering Preparation Thread**: Prepare notation rendering data off the main thread

Synchronize data access between threads using appropriate concurrency mechanisms (e.g., Kotlin coroutines).

#### 7.2 Memory Management

- Reuse rendering objects to minimize allocations
- Implement object pooling for frequently created/destroyed objects
- Limit history of processed notes to prevent memory growth
- Use WeakReferences for observer patterns

#### 7.3 Battery Optimization

- Adjust rendering frequency when app is not in foreground
- Release MIDI resources when app is in background
- Implement efficient drawing algorithms to minimize CPU usage

### 8. Testing Plan

#### 8.1 Unit Tests

- Test MIDI message parsing with various input data
- Test chord identification with different note combinations
- Test state management with simulated MIDI events
- Test UI component logic with mocked dependencies

#### 8.2 Integration Tests

- Test USB connection/disconnection handling
- Test end-to-end MIDI processing pipeline
- Test UI updates in response to MIDI events

#### 8.3 Performance Tests

- Measure and verify latency between MIDI input and display update
- Test memory usage over extended sessions
- Verify frame rate stability under load

#### 8.4 Test Devices and Environments

Minimum test matrix:
- At least 3 different physical Android devices
  - One low-end device (minimum supported specs)
  - One mid-range device
  - One high-end device
- At least 2 different MIDI keyboards
- Test on minimum and target API levels

### 9. Implementation Plan

#### 9.1 Phase 1: Core Infrastructure

1. Set up project structure and dependencies
2. Implement USB/MIDI subsystem
3. Develop basic note processing logic
4. Create minimal UI for testing

Estimated effort: 2-3 weeks

#### 9.2 Phase 2: Musical Components

1. Implement staff view with proper notation
2. Develop chord identification algorithm
3. Integrate UI components
4. Add support for sustain pedal

Estimated effort: 3-4 weeks

#### 9.3 Phase 3: Refinement and Testing

1. Optimize performance
2. Enhance error handling
3. Implement comprehensive testing
4. Polish UI and UX

Estimated effort: 2-3 weeks

### 10. Key Libraries and Dependencies

- **Android MIDI API**: For MIDI device communication
- **AndroidX**: Core UI components and architecture components
- **Kotlin Coroutines**: For asynchronous processing
- **Timber**: For logging
- **Custom view implementations**: For staff notation (no third-party library)

### 11. Future Enhancements (Out of Scope for Initial Version)

- Advanced jazz chord identification
- Note and chord history recording
- Visual effects and animations
- MIDI output capabilities
- Custom UI themes
- Transposition features
- Tablet-optimized layouts

### 12. Appendix

#### 12.1 MIDI Note Number Reference

| Note | MIDI Number | Note | MIDI Number |
|------|-------------|------|-------------|
| C-1  | 0           | C3   | 60 (Middle C) |
| C0   | 12          | C4   | 72          |
| C1   | 24          | C5   | 84          |
| C2   | 36          | C6   | 96          |

#### 12.2 Chord Type Patterns

| Chord Type | Note Pattern | Example (C) |
|------------|--------------|-------------|
| Major      | 1, 3, 5      | C, E, G     |
| Minor      | 1, ♭3, 5     | C, E♭, G    |
| Dominant 7 | 1, 3, 5, ♭7  | C, E, G, B♭ |
| Major 7    | 1, 3, 5, 7   | C, E, G, B  |
| Minor 7    | 1, ♭3, 5, ♭7 | C, E♭, G, B♭ |
| Diminished | 1, ♭3, ♭5    | C, E♭, G♭   |
| Augmented  | 1, 3, ♯5     | C, E, G♯    |
| Sus2       | 1, 2, 5      | C, D, G     |
| Sus4       | 1, 4, 5      | C, F, G     |
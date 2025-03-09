# Planning for USB MIDI Connection Troubleshooting and Fixes

Based on analysis of the code and error messages shown in the screenshots, we've identified several issues with the USB MIDI implementation. The following plan addresses these problems:

## Current Issues Identified
- Status shows "Disconnected" and "Error" despite detecting the MIDI device
- Error message "Failed to open midi input port" despite successfully connecting to the USB device path
- Potential race conditions or improper handling of the MIDI connection lifecycle
- Trying to use both MidiManager API and direct USB connection without proper coordination

## Action Plan

1. [x] Examine Current Implementation
   - Reviewed MidiDeviceManagerImpl.kt and identified several potential issues
   - Examined Android Manifest for correct permissions and USB device filters
   - Verified MidiInputProcessor implementation

2. [ ] Fix MidiDeviceManagerImpl Connection Logic
   - Simplify connection strategy - choose either MidiManager or direct USB consistently 
   - Fix error in setupMidiInput() method that's causing "Failed to open midi input port"
   - Fix race conditions between device detection and connection attempts
   - Improve error handling with more specific error messages
   - Add proper synchronization for thread safety

3. [ ] Improve Error Handling and Reporting
   - Enhance error diagnostics by adding more detailed error messages
   - Create a more robust error recovery mechanism
   - Implement proper error state management
   - Add retry mechanism with backoff for transient connection issues

4. [ ] Fix Permission and Thread Management
   - Ensure USB permissions are properly requested and handled
   - Improve thread management for MIDI I/O operations
   - Use dedicated HandlerThread with proper lifecycle management
   - Fix resource leaks that might cause disconnection issues

5. [ ] Enhance Device Detection and Connection Stability
   - Improve USB device filtering to avoid connecting to incompatible devices
   - Verify USB interface and endpoint selection logic
   - Add proper validation of MIDI interfaces before connection attempts
   - Implement more robust device matching between UsbDevice and MidiDeviceInfo

6. [ ] Follow Android MIDI Best Practices
   - Implement according to official Android USB MIDI documentation
   - Reference Google's MIDI samples and USB MIDI implementations
   - Follow proper lifecycle management for Android MIDI connections
   - Use Android's recommended threading model for MIDI processing

7. [ ] Testing and Verification
   - Test connection with multiple MIDI device types
   - Verify reconnection behavior works properly
   - Test USB device hot-plugging scenarios
   - Verify error recovery mechanisms

## Implementation Details

### 1. First Priority Fixes
- Fix the input port opening issue in setupMidiInput() - likely the cause of "Failed to open midi input port"
- Fix the state management to prevent conflicting "Connected" + "Error" states
- Simplify the connection strategy to avoid mixing MidiManager and direct USB approaches
- Add better error trapping around critical operations

### 2. Second Priority Improvements
- Improve the device detection and matching between USB and MIDI APIs
- Enhance thread safety and synchronization
- Add more comprehensive logging to track connection state transitions
- Improve cleanup to prevent resource leaks

### 3. Reference Resources
- Android official MIDI documentation: https://developer.android.com/reference/android/media/midi/package-summary
- Android USB Host documentation: https://developer.android.com/guide/topics/connectivity/usb/host
- Google's MIDI samples in Android SDK samples
- Third-party open-source MIDI libraries for reference implementations

Next Steps:
1. Implement the highest priority fixes in MidiDeviceManagerImpl
2. Test with physical MIDI devices after each fix
3. Progressively improve the implementation following the plan

## Steps to Resolve MIDI Event Connection without Manual Refresh

- [x] Identify that MIDI events only work after a manual refresh.
- [x] Revert to a commit where MIDI events were working.
- [x] Implement auto scanning for USB devices in onResume of MainActivity.
- [x] Modify scanForUsbDevices to automatically connect if only one device is available.
- [x] Add a delay for refreshAvailableDevices after auto scanning to allow connection to settle.
- [ ] Test auto-connection to ensure that status shows "Connected" without manual refresh.
- [ ] Verify that error messages ("Disconnected" / "Error") no longer appear on app start.
- [ ] Document the changes and update user instructions if necessary.

## Chord Detection Implementation

### Implemented Components
- [x] Create ChordType enum with intervals for various chord types
- [x] Implement Chord data class with root, type, inversion, and naming methods
- [x] Create ChordIdentifier interface and ChordListener for notifications
- [x] Implement BasicChordIdentifier for standard chord detection
- [x] Implement AdvancedChordIdentifier with partial matching for complex chords

### Tests for Chord Identification
- [x] Write BasicChordIdentifierTest with tests for common chord types
  - [x] Test major and minor triads
  - [x] Test chord inversions
  - [x] Test 7th chords (dominant, major, minor)
  - [x] Test augmented and diminished chords
  - [x] Test suspended chords
- [x] Write AdvancedChordIdentifierTest with tests for complex scenarios
  - [x] Test partial chord detection (missing notes)
  - [x] Test extended chords (9th, 11th, 13th)
  - [x] Test altered chords (flat 5, sharp 5, etc.)
  - [x] Test unusual voicings

### Next Steps for Chord Detection
- [ ] Run tests to verify both basic and advanced chord detection
- [ ] Integrate ChordIdentifier with the MainViewModel
- [ ] Connect ChordIdentifier to the UI for chord name display
- [ ] Add visualization of detected chords on the staff view
- [ ] Implement support for alternative chord names (flat vs. sharp notation)
- [ ] Add user settings for chord detection sensitivity
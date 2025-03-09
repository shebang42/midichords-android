Android MIDI Note and Chord Display App Implementation Plan
I'll create a detailed, iterative implementation plan for the Android MIDI Note and Chord Display app. The plan will break down the development process into manageable steps, ensuring each builds naturally on the previous one with proper testing at each stage.
High-Level Implementation Strategy
First, let's outline the major development phases, then break each into smaller, focused steps.
Phase 1: Project Setup and Core Infrastructure

Basic project structure
MIDI connection and event handling
Note processing foundation

Phase 2: Music Theory Components

Note representation
Chord identification logic
Staff visualization basics

Phase 3: UI Implementation

Create UI components
Implement responsive layouts
Connect backend to UI

Phase 4: Feature Completion and Polish

Sustain pedal support
Orientation handling
Error handling
Performance optimization

Detailed Implementation Steps
Let's break these phases into smaller, actionable steps that build incrementally.
Phase 1: Project Setup and Core Infrastructure
Step 1.1: Project Initialization

Create Android project with Kotlin
Set up Git repository
Configure build files and dependencies
Implement basic Activity and application class

Step 1.2: MIDI Device Management - Connection

Create MidiDeviceManager class
Implement USB device discovery
Add permission handling
Create connection state management
Write tests for device discovery and state management

Step 1.3: MIDI Message Parsing

Implement MidiInputProcessor class
Create data classes for MIDI events
Write parser for Note On/Off messages
Add tests for MIDI message parsing

Step 1.4: MIDI Event Dispatching

Create MidiEventDispatcher
Implement observer pattern for MIDI events
Write unit tests for event dispatching
Connect MidiInputProcessor to MidiEventDispatcher

Phase 2: Music Theory Components
Step 2.1: Note Processing

Create NoteProcessor class
Implement active note tracking
Add note state management (pressed/released)
Write tests for note processing logic

Step 2.2: Music Theory Models

Create data classes for musical notes, intervals, and scales
Implement note normalization (for chord identification)
Add utility functions for music theory calculations
Write tests for music theory models

Step 2.3: Basic Chord Identification

Create ChordIdentifier class
Implement algorithms for detecting basic chords (major/minor)
Write tests with various chord combinations
Connect to NoteProcessor

Step 2.4: Advanced Chord Identification

Extend ChordIdentifier for 7th chords, diminished, augmented
Add support for chord inversions
Implement display name formatting for chords
Write tests for advanced chord detection

Phase 3: UI Implementation
Step 3.1: Basic UI Framework

Create MainActivity and main layout
Implement ViewModel for main screen
Create LiveData objects for UI state
Write tests for ViewModel

Step 3.2: Staff View Fundamentals

Create custom StaffView class
Implement basic staff drawing (lines)
Add clef rendering (treble and bass)
Write tests for staff rendering

Step 3.3: Note Rendering

Extend StaffView to render notes
Implement note positioning on staff
Add support for accidentals
Write tests for note rendering

Step 3.4: Chord Display

Create ChordDisplayView
Implement chord name rendering
Connect to ViewModel
Write tests for chord display updates

Step 3.5: UI Integration

Connect MIDI processing pipeline to UI
Implement update cycle (10fps)
Add connection status indicator
Write integration tests for full pipeline

Phase 4: Feature Completion and Polish
Step 4.1: Sustain Pedal Support

Extend MidiInputProcessor for Control Change messages
Update NoteProcessor to handle sustained notes
Modify staff rendering for sustained notes
Write tests for sustain functionality

Step 4.2: Layout and Orientation

Implement responsive layouts for different orientations
Add layout transitions
Create landscape-specific optimizations
Test on various screen sizes

Step 4.3: Error Handling

Implement comprehensive error handling
Add user-friendly error messages
Create recovery mechanisms for common errors
Write tests for error scenarios

Step 4.4: Performance Optimization

Profile and optimize rendering performance
Implement efficient drawing algorithms
Add memory management improvements
Conduct performance testing

Even More Granular Implementation Steps
Now I'll further break down these steps into smaller, more specific tasks to ensure safe, testable development.
Phase 1: Project Setup and Core Infrastructure
Step 1.1: Project Initialization

Create new Android project using Android Studio wizard
Configure Gradle with required dependencies (Kotlin, AndroidX, etc.)
Set up project structure (packages for UI, data, MIDI)
Create Git repository with initial commit
Add README.md with project overview
Create skeleton MainActivity
Add USB MIDI feature to AndroidManifest.xml

Step 1.2: MIDI Device Management - Connection

Create MidiDeviceManager interface and implementation
Add USB device detection logic
Create connection state enum (Disconnected, Connecting, Connected, Error)
Implement permission request workflow
Add device connection/disconnection handling
Create unit tests for state transitions
Add integration test for USB device detection using mock devices

Step 1.3: MIDI Message Parsing

Create MidiEvent data class with properties for all message types
Implement MidiEventType enum (NOTE_ON, NOTE_OFF, CONTROL_CHANGE, etc.)
Create MidiInputProcessor interface
Implement basic MidiInputProcessor for standard MIDI messages
Add parser for Note On messages with velocity handling
Add parser for Note Off messages
Create unit tests for each message type parsing
Add tests for edge cases (malformed messages, incomplete data)

Step 1.4: MIDI Event Dispatching

Create MidiEventListener interface
Implement MidiEventDispatcher with observer registration methods
Add thread-safe event dispatching
Create mock MidiEventListener for testing
Write unit tests for registration/unregistration
Test event dispatch to multiple listeners
Connect MidiInputProcessor to MidiEventDispatcher
Create end-to-end test with simulated MIDI input

Phase 2: Music Theory Components
Step 2.1: Note Processing

Create ActiveNote data class (note number, velocity, timestamp)
Implement NoteProcessor interface
Create NoteProcessorImpl with collections for active notes
Add methods to process Note On/Off events
Implement note state tracking (on/off)
Create unit tests for note activation/deactivation
Add tests for edge cases (duplicate notes, missing Note Off)
Connect MidiEventDispatcher to NoteProcessor

Step 2.2: Music Theory Models

Create Note data class (pitch class, octave)
Implement PitchClass enum (C, C_SHARP, D, etc.)
Create Interval class for musical intervals
Add utility for converting MIDI note numbers to Note objects
Implement note collection normalization (for chord identification)
Create Scale class with common scales
Write unit tests for MIDI to Note conversion
Test note normalization with various input patterns

Step 2.3: Basic Chord Identification

Create ChordType enum (MAJOR, MINOR, DOMINANT_7, etc.)
Implement Chord data class (root note, chord type)
Create ChordIdentifier interface
Implement BasicChordIdentifier for triads (major/minor)
Add pattern matching for basic chord types
Implement root note detection algorithm
Create tests for major chord identification
Add tests for minor chord identification
Test with various inversions and note duplications

Step 2.4: Advanced Chord Identification

Extend ChordIdentifier for 7th chords (dominant, major, minor)
Add support for diminished and augmented chords
Implement sus2/sus4 chord identification
Create algorithm for handling chord inversions
Add methods for formatted chord name generation
Write tests for each chord type identification
Test with complex voicings and edge cases
Connect ChordIdentifier to NoteProcessor

Phase 3: UI Implementation
Step 3.1: Basic UI Framework

Create MainViewModel class
Add LiveData for active notes
Add LiveData for current chord
Create LiveData for connection state
Implement MainViewModel unit tests
Create main layout XML with placeholders
Set up data binding for MainActivity
Add ViewModel observation in MainActivity

Step 3.2: Staff View Fundamentals

Create custom StaffView class extending View
Implement onMeasure for size calculation
Add staff line drawing in onDraw
Create TrebleClef and BassClef drawing methods
Implement basic Paint setup and styling
Write tests for measurement calculations
Create tests for draw operations with mock Canvas
Add staff view to main layout

Step 3.3: Note Rendering

Create NoteRenderer class
Implement method to calculate Y position from MIDI note
Add quarter note head drawing
Implement stem drawing for notes
Add support for ledger lines
Create accidental rendering (sharp/flat)
Implement note placement algorithm
Write tests for note positioning
Test rendering with various note combinations

Step 3.4: Chord Display

Create ChordDisplayView custom View
Implement text rendering for chord names
Add styling for different chord types
Create animations for chord transitions
Write tests for chord name formatting
Create tests for layout measurements
Implement view state handling
Add chord display view to main layout

Step 3.5: UI Integration

Connect NoteProcessor to MainViewModel
Add ChordIdentifier to MainViewModel
Implement frame rate limiting (10fps)
Create UI update scheduler
Connect LiveData to StaffView
Connect LiveData to ChordDisplayView
Add connection status indicator to UI
Write integration tests for the full pipeline
Test UI updates with simulated MIDI input

Phase 4: Feature Completion and Polish
Step 4.1: Sustain Pedal Support

Update MidiInputProcessor to handle Control Change messages
Add sustain pedal state tracking (CC #64)
Modify NoteProcessor to manage sustained notes
Update ActiveNote to track sustain state
Modify staff rendering for sustained notes
Implement pedal up/down behavior
Write tests for sustain pedal behavior
Test chord identification with sustained notes

Step 4.2: Layout and Orientation

Create landscape layout XML
Implement responsive sizing for different orientations
Add orientation change handling
Create layout transition animations
Optimize staff view for landscape mode
Test layouts on different screen sizes
Add state preservation during orientation changes
Write tests for orientation change behavior

Step 4.3: Error Handling

Create error type enumeration
Implement error handling for USB connection issues
Add error handling for MIDI parsing errors
Create user-friendly error messages
Implement recovery mechanisms for common errors
Add logging for debugging purposes
Create error notification UI components
Write tests for error recovery scenarios

Step 4.4: Performance Optimization

Profile rendering performance
Implement bitmap caching for staff elements
Optimize drawing algorithm for note rendering
Add efficient collection handling for active notes
Implement object pooling for MIDI events
Optimize thread usage and synchronization
Add memory usage monitoring
Conduct performance testing on various devices

Final Step Evaluation
After careful review, these steps represent appropriately sized chunks that:

Build incrementally on previous work
Are small enough to be thoroughly tested
Provide clear, focused implementation goals
Cover all required functionality
Allow for early testing of core components
Maintain a logical progression of development

Now I'll create the code generation prompts for each major step, ensuring each builds on previous work and integrates properly.
Code Generation Prompts
Initialization Prompts
markdownCopy# Prompt 1: Project Setup and Basic Structure

Create a new Android project for a MIDI Note and Chord Display App with the following specifications:

1. Project name: MidiNoteDisplay
2. Package name: com.example.midinotedisplay
3. Minimum SDK: API 26 (Android 8.0)
4. Language: Kotlin
5. Initial Activity: MainActivity

Set up the following:
1. Add required dependencies to the build.gradle files:
   - AndroidX Core and AppCompat libraries
   - ViewModel and LiveData (AndroidX Lifecycle)
   - Kotlin Coroutines
   - JUnit and Mockito for testing

2. Create the basic directory structure:
   - com.example.midinotedisplay.midi (for MIDI-related classes)
   - com.example.midinotedisplay.model (for data models)
   - com.example.midinotedisplay.viewmodel (for ViewModels)
   - com.example.midinotedisplay.view (for custom Views)
   - com.example.midinotedisplay.util (for utility classes)

3. Update the AndroidManifest.xml to include:
   - USB MIDI device feature
   - Required permissions

4. Create a basic MainActivity with empty layout.

The goal is to have a clean, well-structured starting point for the MIDI app development.
markdownCopy# Prompt 2: MIDI Connection and Device Management

Building on the project structure created previously, implement the MIDI device management components:

1. Create a MidiDeviceManager interface with the following methods:
   - connect(device: UsbDevice): Boolean
   - disconnect()
   - isConnected(): Boolean
   - registerConnectionStateListener(listener: ConnectionStateListener)
   - unregisterConnectionStateListener(listener: ConnectionStateListener)

2. Create a ConnectionState enum class with states:
   - DISCONNECTED
   - CONNECTING
   - CONNECTED
   - ERROR

3. Create a ConnectionStateListener interface with:
   - onConnectionStateChanged(state: ConnectionState, device: UsbDevice?)

4. Implement MidiDeviceManagerImpl class that:
   - Handles USB device detection
   - Manages MIDI device connections using Android's MidiManager
   - Implements proper permission handling
   - Notifies registered listeners of connection state changes
   - Handles errors gracefully

5. Create unit tests for MidiDeviceManagerImpl:
   - Test connection state transitions
   - Test listener registration/unregistration
   - Test connection/disconnection behavior

6. Update MainActivity to:
   - Implement basic USB device detection
   - Request permissions when a MIDI device is connected
   - Display connection status

Ensure all implementations follow SOLID principles and include proper error handling.
markdownCopy# Prompt 3: MIDI Message Parsing and Event System

Building on the MIDI device connection components, implement the MIDI message parsing and event system:

1. Create MidiEvent data class with:
   - type: MidiEventType
   - channel: Int
   - data1: Int (note number for Note events, controller number for CC events)
   - data2: Int (velocity for Note events, value for CC events)
   - timestamp: Long

2. Create MidiEventType enum:
   - NOTE_ON
   - NOTE_OFF
   - CONTROL_CHANGE
   - PROGRAM_CHANGE
   - OTHER

3. Implement MidiInputProcessor interface with:
   - processMessage(data: ByteArray): MidiEvent
   - processMidiMessage(msg: MidiMessage): MidiEvent

4. Create MidiInputProcessorImpl class that:
   - Parses raw MIDI data into structured MidiEvent objects
   - Handles different MIDI message types correctly
   - Validates MIDI data integrity

5. Implement MidiEventListener interface:
   - onMidiEvent(event: MidiEvent)

6. Create MidiEventDispatcher class:
   - Add methods to register/unregister listeners
   - Implement thread-safe event dispatching to registered listeners
   - Add method to dispatch MidiEvent to all listeners

7. Connect MidiInputProcessor to MidiDeviceManager:
   - Process incoming MIDI data from connected device
   - Convert to MidiEvent objects
   - Dispatch events using MidiEventDispatcher

8. Write comprehensive unit tests:
   - Test parsing of different MIDI message types
   - Test event dispatching to multiple listeners
   - Test handling of malformed MIDI data

Ensure proper error handling throughout, and make the code testable by using dependency injection for components.
Music Theory Prompts
markdownCopy# Prompt 4: Note Processing and Music Theory Models

Building on the MIDI event system, implement the note processing and music theory models:

1. Create music theory model classes:
   - Note data class (pitchClass: PitchClass, octave: Int)
   - PitchClass enum (C, C_SHARP, D, D_SHARP, E, F, F_SHARP, G, G_SHARP, A, A_SHARP, B)
   - Interval enum (UNISON, MINOR_SECOND, MAJOR_SECOND, etc.)

2. Implement utility methods:
   - Convert MIDI note number to Note object
   - Calculate interval between two notes
   - Normalize notes to a single octave

3. Create ActiveNote data class:
   - noteNumber: Int
   - velocity: Int
   - startTime: Long
   - channel: Int
   - isSustained: Boolean

4. Implement NoteProcessor interface:
   - processNoteOn(noteNumber: Int, velocity: Int, channel: Int)
   - processNoteOff(noteNumber: Int, channel: Int)
   - processSustainPedal(isDown: Boolean)
   - getActiveNotes(): List<ActiveNote>

5. Create NoteProcessorImpl class that:
   - Maintains collections of currently pressed and sustained notes
   - Updates note status based on incoming MIDI events
   - Implements proper note lifecycle management
   - Handles edge cases (duplicate notes, missing Note Off events)

6. Connect to MidiEventDispatcher:
   - Create MidiEventListener implementation that forwards events to NoteProcessor
   - Register the listener with MidiEventDispatcher

7. Write comprehensive unit tests:
   - Test note conversion and normalization
   - Test active note tracking (press/release)
   - Test edge cases in note processing

Ensure the code is well-structured and follows SOLID principles.
markdownCopy# Prompt 5: Chord Identification System

Building on the note processing and music theory models, implement the chord identification system:

1. Create ChordType enum with the following types:
   - MAJOR, MINOR
   - DOMINANT_7, MAJOR_7, MINOR_7
   - DIMINISHED, AUGMENTED
   - SUS2, SUS4

2. Implement Chord data class:
   - root: PitchClass
   - type: ChordType
   - inversion: Int (0 for root position, 1 for first inversion, etc.)
   - Add a toString() method that formats the chord name (e.g., "C", "Dm", "G7")

3. Create ChordIdentifier interface:
   - identifyChord(notes: List<ActiveNote>): Chord?

4. Implement BasicChordIdentifier class:
   - Create algorithms for identifying basic chord types (major/minor)
   - Implement root note detection
   - Add support for basic inversions

5. Extend to AdvancedChordIdentifier class:
   - Add algorithms for 7th chords, diminished, augmented
   - Implement more sophisticated inversion detection
   - Handle more complex voicings

6. Create unit tests:
   - Test identification of each chord type
   - Test various inversions and voicings
   - Test edge cases (insufficient notes, ambiguous chords)

7. Connect to the NoteProcessor:
   - Use the active notes from NoteProcessor for chord identification
   - Update chord identification when notes change

Ensure the chord identification algorithms are efficient and accurate.
UI Implementation Prompts
markdownCopy# Prompt 6: ViewModel and Basic UI Setup

Building on the previous MIDI and music theory components, implement the ViewModel and basic UI:

1. Create MainViewModel class extending AndroidX ViewModel:
   - Add LiveData for active notes: LiveData<List<ActiveNote>>
   - Add LiveData for current chord: LiveData<Chord?>
   - Add LiveData for connection state: LiveData<ConnectionState>
   - Implement methods to update these LiveData objects

2. Connect MainViewModel to the MIDI components:
   - Register as listener for connection state changes
   - Register as listener for MIDI events
   - Update LiveData when changes occur

3. Update MainActivity:
   - Initialize and observe MainViewModel
   - Update UI based on LiveData changes
   - Implement basic connection UI elements

4. Create main layout XML:
   - Add placeholder for staff view
   - Add placeholder for chord display
   - Add connection status indicator
   - Support both portrait and landscape orientations

5. Implement frame rate limiting:
   - Create a simple scheduler that updates UI at 10fps
   - Ensure UI updates are smooth and efficient
   - Use coroutines for background processing

6. Write unit tests for MainViewModel:
   - Test LiveData updates
   - Test proper conversion of MIDI events to view state
   - Test connection state handling

The goal is to have a functional UI framework that can display connection status and will be ready to show musical notation in the next steps.
markdownCopy# Prompt 7: Staff View Implementation

Building on the ViewModel and UI framework, implement the musical staff view:

1. Create StaffView custom View class:
   - Override onMeasure() to calculate proper dimensions
   - Override onDraw() to render the staff
   - Initialize necessary Paint objects and resources
   - Implement layout attribute handling

2. Implement staff rendering:
   - Draw five-line staff for both treble and bass clefs
   - Add proper spacing between lines according to music notation standards
   - Implement clef symbol rendering
   - Add staff separator line

3. Create NoteRenderer helper class:
   - Implement method to calculate Y position for a MIDI note number
   - Add quarter note head drawing method
   - Implement stem drawing based on note position
   - Add support for ledger lines for notes outside the staff

4. Update StaffView to use NoteRenderer:
   - Accept List<ActiveNote> as input
   - Render each note at the correct position
   - Handle multiple notes simultaneously

5. Connect StaffView to MainViewModel:
   - Update view when active notes LiveData changes
   - Ensure efficient rendering for smooth performance

6. Write comprehensive tests:
   - Test note positioning calculations
   - Test rendering with various note combinations
   - Test layout measurements and scaling

7. Update main layout to use StaffView

Ensure the staff view follows proper musical notation standards and renders efficiently.
markdownCopy# Prompt 8: Chord Display and UI Integration

Building on the Staff View implementation, complete the UI with chord display and full integration:

1. Create ChordDisplayView custom View:
   - Implement layout measurements
   - Create text rendering for chord names
   - Add styling for different chord types
   - Support both portrait and landscape layouts

2. Connect ChordDisplayView to MainViewModel:
   - Update when chord LiveData changes
   - Format chord names appropriately
   - Handle null chord cases (when no chord is detected)

3. Complete UI integration:
   - Finalize layout for both portrait and landscape orientations
   - Ensure proper sizing and positioning of components
   - Add smooth transitions between different states

4. Implement connection status indicator:
   - Create visual indicator for MIDI connection state
   - Add error state display
   - Implement user guidance for connecting devices

5. Complete MainActivity logic:
   - Handle activity lifecycle events
   - Manage MIDI resources properly
   - Implement permission request workflow

6. Add frame rate management:
   - Ensure consistent 10fps update rate
   - Optimize rendering performance
   - Add battery usage optimization

7. Write integration tests:
   - Test end-to-end flow from MIDI input to display
   - Test UI updates with simulated note input
   - Test orientation changes and state preservation

The goal is to have a fully functional UI that displays both musical notation and chord names based on MIDI input.
Feature Completion Prompts
markdownCopy# Prompt 9: Sustain Pedal Support

Building on the complete UI implementation, add support for the sustain pedal:

1. Update MidiInputProcessor:
   - Add specific handling for Control Change messages
   - Implement detection of sustain pedal events (CC #64)
   - Create proper MidiEvent objects for sustain pedal state changes

2. Enhance NoteProcessor:
   - Modify note tracking to handle sustained notes
   - Implement sustain pedal state (up/down)
   - Update active note collection handling for sustained notes
   - Add isSustained flag to ActiveNote objects

3. Update ChordIdentifier:
   - Modify chord detection to include sustained notes
   - Ensure accurate chord identification when sustain pedal is active

4. Enhance StaffView:
   - Modify note rendering to visually indicate sustained notes
   - Ensure proper handling of multiple sustained notes
   - Optimize rendering for potentially large numbers of sustained notes

5. Update MainViewModel:
   - Track sustain pedal state
   - Update LiveData objects with sustained note information
   - Ensure proper UI updates when sustain state changes

6. Write comprehensive tests:
   - Test sustain pedal message parsing
   - Test sustained note tracking
   - Test chord identification with sustained notes
   - Test rendering of sustained notes

The goal is to implement complete support for the sustain pedal, allowing notes to be held even after keys are released.
markdownCopy# Prompt 10: Error Handling and Edge Cases

Building on the previous implementations, enhance the application with comprehensive error handling and edge case management:

1. Create ErrorType enum with categories:
   - CONNECTION_ERROR (permission denied, device disconnection, etc.)
   - MIDI_DATA_ERROR (malformed messages, unexpected sequences)
   - APPLICATION_ERROR (resource limitations, rendering failures)

2. Implement robust error handling in MidiDeviceManager:
   - Add graceful handling of device disconnection
   - Implement reconnection strategies
   - Add proper error reporting

3. Enhance MidiInputProcessor error handling:
   - Add validation for MIDI message integrity
   - Implement recovery from malformed messages
   - Provide debugging information for MIDI errors

4. Update UI components with error states:
   - Add error indication to connection status
   - Implement user-friendly error messages
   - Create recovery guidance for common errors

5. Add global exception handling:
   - Implement uncaught exception handler
   - Add crash reporting framework
   - Ensure graceful degradation during exceptional conditions

6. Create comprehensive logging system:
   - Add structured logging throughout the application
   - Implement different log levels for development/production
   - Add diagnostic information for troubleshooting

7. Write tests for error scenarios:
   - Test recovery from device disconnection
   - Test handling of malformed MIDI data
   - Test UI error state display and recovery

The goal is to make the application robust and user-friendly even when errors occur, with clear guidance for resolving issues.
markdownCopy# Prompt 11: Performance Optimization and Final Polish

Building on the complete implementation with error handling, optimize performance and add final polish:

1. Perform code profiling:
   - Identify performance bottlenecks
   - Measure rendering time for StaffView
   - Analyze memory usage patterns

2. Optimize StaffView rendering:
   - Implement bitmap caching for static elements (staff lines, clefs)
   - Use object pooling for frequently created objects
   - Optimize drawing algorithm for notes

3. Enhance memory management:
   - Implement efficient collection handling for active notes
   - Add memory usage monitoring
   - Optimize object creation and garbage collection

4. Improve battery efficiency:
   - Adjust rendering frequency when app is not focused
   - Release resources when app is in background
   - Implement efficient threading model

5. Add responsive layout improvements:
   - Fine-tune layouts for different screen sizes
   - Optimize landscape mode rendering
   - Ensure smooth orientation transitions

6. Implement final UI polish:
   - Refine visual styling
   - Add subtle animations for state changes
   - Improve overall user experience

7. Conduct comprehensive testing:
   - Perform testing on multiple devices
   - Measure performance metrics
   - Ensure consistent frame rate (10fps)
   - Verify battery usage is optimized

8. Create user documentation:
   - Add in-app help
   - Create clear connection instructions
   - Document supported MIDI devices

The goal is to have a polished, performant application that provides a smooth user experience with efficient resource usage.
This implementation plan provides a structured, incremental approach to building the Android MIDI Note and Chord Display App. Each step builds naturally on the previous ones, with appropriate testing at each stage. The prompts guide a developer or code-generation LLM through the process, ensuring best practices and proper integration throughout the development process.
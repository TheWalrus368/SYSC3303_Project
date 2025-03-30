Fire Incident Management System
===============================

Overview
--------
The Fire Incident Management System is a multi-threaded simulation that models the interaction between fire incidents, drones, and a central scheduler. The system consists of multiple components, including a FireIncidentSubsystem, DroneSubsystem, and Scheduler, which coordinate the detection and response to fire incidents.

This project is implemented in Java and follows an event-driven architecture, with multi-threading and synchronization mechanisms ensuring smooth task delegation between components.

Components
----------
1. FireIncidentSubsystem

- Reads fire events from a CSV file.

- Sends fire events to the Scheduler.

- Waits for drone responses via the Scheduler.

- Sends acknowledgement to scheduler. 

2. DroneSubsystem

- Fetches fire events from the Scheduler.

- Processes the event and simulates firefighting.


3. Scheduler

- Acts as an intermediary between FireIncidentSubsystem and DroneSubsystem.

- Manages task queues for fire event delegation.

- Handles synchronization between event generation and drone responses.

4. DroneStateMachine

- Implements the drone behavior using a state machine.

- The drone can be in various states such as Idle, Responding, Extinguishing, Returning.

- Transitions between states occur based on fire event assignments and completion status.

5. SchedulerStateMachine

- Implements the behavior for the scheduler.

- Scheduler has various states such as DISPATCH_DRONE, NOTIFY_FIRE_EXTINGUISHED, NEW_FIRE, etc

- Transitions between states are based on requests from the DroneSubsystems and FireIncidentSubsystems

UML Diagrams
------------
Class Diagram

The Class Diagram represents the structure of the system, showing relationships between: 1) FireIncidentSubsystem, DroneSubsystem, Scheduler, and DroneStateMachine.

											 2) The FireEvent entity which encapsulates fire event details.

Timing Diagram
The Timing Diagram demonstrates how the drones behave during error scenarios


Unit Tests
-----------

This project includes unit tests for verifying the behavior of each subsystem:

1. ModifiedDroneSubsystem

- A specialized subclass of DroneSubsystem for unit testing.

- Overrides travel simulation to enable controlled movement for testing

2. ModifiedFireIncidentSubsystem

- A specialized subclass of FireIncidentSubsystem for unit testing.

- Overrides data ingestion to allow predefined fire event scenarios for testing

3. ModifiedScheduler

- A specialized subclass of Scheduler for unit testing.

- Overrides task assignment logic to prioritize test scenarios

4. TestSystem

- A test class that handles all testing across system

- Tests the behaviour of DroneSubsystem, Scheduler, and FireIncidentSubsystem


Running the Simulation
----------------------

Prerequisites:

- Java 17+

- JUnit 5 for testing

- Gradle for building the project

- JDK 21


Build and Execution Steps
-------------------------

1) Compile and run separate classes in this specific order: Scheduler.java, FireEventSubsystem.java, DroneSubsystem.java

2) Ensure the CSV file (fire_events.csv) contains sample fire incidents.

3) Observe logs for fire detection, drone response, and acknowledgments.


Running Unit Tests
------------------
- Compile and run test cases.
- This runs all JUnit 5 test cases to validate the system behavior.


Conclusion
-----------
This project simulates a fire incident response system with multi-threading, state machines, and synchronized task delegation. The provided UML diagrams help visualize system interactions and behaviors, ensuring clarity in implementation.

Authors
-------
README written by Claire and JavaDocs written by Brian and Darren
Developed by Darren and Brian (main code) and Claire and Tony (test cases)
UML Class Diagram by Tony and Timing Diagram by Daniel
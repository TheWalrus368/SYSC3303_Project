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


UML Diagrams
------------
Class Diagram

The Class Diagram represents the structure of the system, showing relationships between: 1) FireIncidentSubsystem, DroneSubsystem, Scheduler, and DroneStateMachine.

											 2) The FireEvent entity which encapsulates fire event details.

											 3) TestScheduler, which extends Scheduler for unit testing.

Unit Tests
-----------

This project includes unit tests for verifying the behavior of each subsystem:

1. DroneSubsystemTest

- Ensures that the DroneSubsystem correctly fetches tasks from the Scheduler.

- Validates that processed fire events are acknowledged correctly.

2. FireIncidentSubsystemTest

- Verifies that fire events are correctly parsed from a CSV file.

- Checks that events are sent to the Scheduler and processed correctly.

3. SchedulerTest

- Tests event reception, task delegation, and acknowledgment forwarding.

- Ensures synchronization in task handling.

4. TestScheduler

- A specialized subclass of Scheduler for unit testing.

- Tracks sent events and responses to verify system behavior.


Running the Simulation
----------------------

Prerequisites:

- Java 17+

- JUnit 5 for testing

- Gradle for building the project

- JDK 21


Build and Execution Steps
-------------------------

1) Compile and run Simulation.java to start the system

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
README and Javadocs written by Tony Situ
Developed by Darren and Brian (main code) and Daniel (test cases)
UML Diagrams by Claire (Class Diagram)
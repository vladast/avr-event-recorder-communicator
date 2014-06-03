# AVR Event Recorder's Android-based Front-end

Android-based front-end for AVR-based event recorder device

* **Project code name**: Fisherman's viewer

## Description

This Android project is PoC project for communication with AVR-based USB device.
In particular, resulting Andorid application is able to communicate with AVR-based event recorder. More on AVR-based event recorder device can be found on: https://github.com/vladast/avr-based-event-recorder-with-usb-support

## Features

In this stage for pre-release version 0.0.1, following features were implemented:
* USB host implementation
* USB communication with custom AVR-based device
* Download of recorded events from AVR device
* Display of recorded events in tabular form
* Sharing downloaded events in CSV form with other application present on Android device (type extra data shared with other Android intents is text/text)
* Re-initiation of connected USB device (preparing AVR device for new recording session)
* Complete separation of communication logic and UI (event-based communication between modules)
**NOTE**: Application's UI is bare as it can be - for this pre-release, it's only intended to display data in tabular form and offer share/re-init options.

## Operation

Without AVR-based USB device connected, application is put in waiting state, constantly checking if any USB devices are connected.
When AVR-based device gets connected, it will be recognized by application (embedded Android USB host feature) and communication will start.
At first, common data such as device name, session number, and number of recorded events are going to be read. Later on, all recorded events are going to be read.
Asynchronously, after each recorded event is read, table row is being added - in general, this is something that is not going to be noticed by end-user, since data download happens at fast rate.

## Build procedure

Having in mind that there are some restrictions in regards of USB's VID/PID properties, build procedure is not as straight as it could be - some manual file edits are still needed.
Build procedure is as follows:
* Clone GitHub-hosted project from https://github.com/vladast/avr-event-recorder-communicator
* Import Android project via ADT/Eclipse (File > Import > General > Existing Projects into Workspace > select root directory of cloned project > Finish)
* From ADT/Eclipse, open **device_filter.xml** file located in res/xml directory.
* Substitute **VID_dec** and **PID_dec** with decimal representation of your AVR-based device's VID and PID values.
* Build ADT/Eclipse project.

## More on VID/PID configuration

To avoid various non-technical issues with VID/PID values, those are not being hard-coded into above-mentioned device_filter.xml file. Similar thing is done in AVR project - you are free to embed your own VID/PID values.
As far as need for decimal representation of VID/PID values is concerned, it's the way Android USB host feature works. Usually, VID/PID values are given in hexadecimal form. Those hexadecimal values should be converted from hexadecimal to decimal when used in Android.
## Author and Licence

**avr-event-recorder-communicator** repository is maintained by *Vladimir Stankovic*.

This project is licensed under *GPL Version 2*. Please, refer to LICENSE file for the full text of the license.

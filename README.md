# Anki_MQTT_Interface

Interface to enable contoling an Anki Overdrive Modell over a MQTT Broker.

*See [anki/drive-sdk](https://github.com/anki/drive-sdk) for the official
SDK written in C.*

### Disclaimer
The authors of this software are in no way affiliated to Anki.
All naming rights for Anki, Anki Drive and Anki Overdrive are property of
[Anki](http://anki.com).

This is a forked repository from [tenbergen/anki-drive-java](https://github.com/tenbergen/anki-drive-java).
The source repository is [adessoAG/anki-drive-java](https://github.com/adessoAG/anki-drive-java), which
appears to be abandoned.

## About

Unfortunately, there is currently no cross-platform Java library to interface
with Bluetooth LE devices.

This project therefore requires a Node.js gateway service to handle low-level
communication with the Anki vehicles. All data processing and message parsing
is carried out in Java code.

## Prerequisites

To build and use the interface in your own project you will need:

- Java JDK (>= 1.8.0)
- a compatible Bluetooth 4.0 interface with LE support

## Installation

To install the interface and all required dependencies run the following commands:

```
git clone https://github.com/Sakaeo/Anki_MQTT_Interface
cd Anki_MQTT_Interface
npm install
./gradlew build
```

## Usage

Start the Node.js gateway service:
```
./gradlew server
```

Start the Host service:
```
./gradlew fatJar	
java -jar build/libs/Anki_MQTT_Interface.jar localhost
```

Or run the Startscript, to start both services:
```
./start.sh localhost
```

localhost: -> Or IP address of the MQTT Broker

### Test File
To test the interface run the .html example:
```
src.Examples.HTML.mqtt.html
```

## Contributing

Contributions are always welcome! Feel free to fork this repository and submit
a pull request.

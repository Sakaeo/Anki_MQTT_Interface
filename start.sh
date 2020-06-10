#!/bin/bash
# Startscript for the Anki-MQTT interface
# Strübin Jan, 11.06.20

TIMEOUT=30

if [ $# -eq 0 ]
then
	echo "start with browerip (ex. localhost)"
	exit 1
fi

echo "starting gateway..."
nohup sudo ./gradlew server & 
sleep $TIMEOUT
echo "starting host service ..."
java -jar build/libs/Anki_MQTT_Interface.jar $1

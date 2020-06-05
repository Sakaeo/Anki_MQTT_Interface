#!/bin/bash

if [ $# -eq 0 ]
then
	echo "start with browerip (ex. localhost)"
	exit 1
fi

echo "starting gateway..."
nohup sudo ./gradlew server & 
sleep 30
echo "starting host service ..."
java -jar build/libs/Anki_MQTT_Interface.jar $1

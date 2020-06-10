package de.jan.anki.host;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Timer;

/**
 * Main class of the Host Service.
 * Strübin Jan, 11.06.20
 */
public class AnkiMain {

    /**
     * Main method of the Host Service
     *
     * @param args Ip address of the MQTT server on port :1883
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("No ip for Mqtt Broker in start argument");
            System.exit(0);
        }

        String mqttServer = "tcp://" + args[0] + ":1883";
        String ankiServer = "localhost";
//        String ankiServer = "169.254.28.205";

        System.out.println("Start");

        try {
            // Get your own IP
            final DatagramSocket socket;
            socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();

            // Open Mqtt connection and set Testament for disgraceful disconnect
            MqttConnectOptions options = new MqttConnectOptions();
            JSONObject json = new JSONObject();
            json.put("value", false);
            options.setWill("Anki/Host/" + ip + "/S/HostStatus", json.toString().getBytes(), 0, true);

            // Create AnkiHost and setup of Mqtt subscriber and publisher for the Host Service
            MQTTPublisher hostPublisher = new MQTTPublisher(mqttServer, "Host/" + ip + "/Publisher", "Anki/Host/" + ip + "/", options);
            AnkiHost ankiHost = new AnkiHost(ankiServer, mqttServer, hostPublisher);
            MQTTSubscriber subscriber = new MQTTSubscriber(ankiHost, mqttServer, "Host/" + ip + "/Subscriber");

            // Fills in the /D Topics
            DescriptionFiller.fill(mqttServer,"Host/" + ip + "/Describer");

            // Individual topic subscriber for host and cars
            subscriber.connect();
            subscriber.subscribe("Anki/Host/" + ip + "/I/#");
            subscriber.subscribe("Anki/Car/+/I/#");

            // Global topic subscriber
            subscriber.subscribe("Anki/Car/I");
            subscriber.subscribe("Anki/Service/I");

            // Pings the online status of the Host Service all 5 seconds
            Timer hostUpdater = new Timer();
            hostUpdater.schedule(new HostPing(hostPublisher), 0, 5000);

            System.out.println("I am going to receive ticks...");
            System.out.println("...press a key to end that.");

            // Keeps the service running
            System.out.println(System.in.read());

            // Sets host status to offline
            hostUpdater.cancel();
            json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("value", false);
            hostPublisher.publish("S/HostStatus", json.toString(), true);

            System.out.println("Done.");

            // Wrap up
            subscriber.disconnect();
            hostPublisher.disconnect();
            System.exit(0);

        } catch (MqttException | IOException | NullPointerException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}



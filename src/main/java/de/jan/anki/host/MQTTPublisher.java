package de.jan.anki.host;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * This class manages the publishing of messages to the Mqtt Broker
 */
public class MQTTPublisher {
    private final String baseTopic;
    private final MqttClient client;

    /**
     * Sets a base Topic, creates a new Mqtt client and invokes the connect() method
     *
     * @param server    Server Ip of the Mqtt broker
     * @param id        Id string for the client id
     * @param baseTopic Base Topic for all publishes of this instance. For Example: "Anki/Car/CarAddress/"
     */
    public MQTTPublisher(String server, String id, String baseTopic) throws MqttException {
        this.baseTopic = baseTopic;

        client = new MqttClient(server, id);
        connect();
    }

    /**
     * Sets a base Topic, creates a new Mqtt client and invokes the connect(option) method
     *
     * @param server    Server Ip of the Mqtt broker
     * @param id        Id string for the client id
     * @param baseTopic Base Topic for all publishes of this instance. For Example: "Anki/Car/CarAddress/"
     * @param options   Options for the Mqtt connection
     */
    public MQTTPublisher(String server, String id, String baseTopic, MqttConnectOptions options) throws MqttException {
        this.baseTopic = baseTopic;

        client = new MqttClient(server, id);
        connect(options);
    }

    /**
     * Connects the Mqtt client to the Mqtt broker
     */
    public void connect() throws MqttException {
        client.connect();
    }

    /**
     * Connects the Mqtt client to the Mqtt broker with additional connection options
     *
     * @param options Options for this connection, For example if a Testament is needed.
     */
    public void connect(MqttConnectOptions options) throws MqttException {
        client.connect(options);
    }

    /**
     * Disconnects the Mqtt client from the Mqtt broker
     */
    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace(); //TODO: Error
        }
    }

    /**
     * Publishes a string as bytes on the extended base topic.
     *
     * @param topic   Topic extension for the base topic
     * @param payload String which  is to be published
     */
    public void publish(String topic, String payload) {
        publish(topic, payload, false);
    }

    /**
     * Publishes a string as bytes on the extended base topic.
     * Has an option to be a retained message.
     *
     * @param topic      Topic extension for the base topic
     * @param payload    String which  is to be published
     * @param isRetained If the message is retained or not
     */
    public void publish(String topic, String payload, boolean isRetained) {
        try {
            client.publish(baseTopic + topic, payload.getBytes(), 0, isRetained);
        } catch (MqttException e) {
            e.printStackTrace(); //TODO: Error?
        }
    }
}

package de.jan.anki.host;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class manages the Mqtt subscriptions and handles the intents received from a client application
 */
public class MQTTSubscriber {
    private final MqttClient client;
    private final AnkiHost service;

    /**
     * Saves the Anki Host and creates a new Mqtt client
     *
     * @param service Anki Host instance in which the subscriber is created
     * @param server  Server ip of the Mqtt broker
     * @param id      Id String for client id
     */
    public MQTTSubscriber(AnkiHost service, String server, String id) throws MqttException {
        this.service = service;

        client = new MqttClient(server, id);
    }

    /**
     * Connects the Mqtt client to the Mqtt broker and registers the responds handler
     */
    public void connect() throws MqttException {
        client.connect();
        client.setCallback(new Handler());
    }

    /**
     * Disconnects the Mqtt client from the Mqtt broker
     */
    public void disconnect() throws MqttException {
        client.disconnect();
    }

    /**
     * Subscribes to a Topic
     *
     * @param topic to which the clients subscribes
     */
    public void subscribe(String topic) throws MqttException {
        client.subscribe(topic);
//        System.out.println("subscribed to: " + topic);
    }

    /*
     * Invokes AnkiHost methods depending on Topic and JSON key words
     */
    class Handler implements MqttCallback {
        @Override
        public void connectionLost(Throwable throwable) {
            System.out.println(throwable.getCause().toString());
            System.out.println("Connection Lost... mqtt Handler");
            System.exit(0);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws MqttException, InterruptedException {
            JSONObject json = new JSONObject();
            try {
                json = new JSONObject(new String(message.getPayload()));
            } catch (JSONException e) {
                e.printStackTrace(); //TODO: Error
                System.out.println("Invalid Json");
            }
            //Host
            if (topic.contains("Host/") || topic.endsWith("Host/I")) {
                if (json.has("connecting")) {
                    if (json.getBoolean("connecting")) {
                        System.out.println("scanCar");
                        service.scanCar(json);
                    } else {
                        System.out.println("Disconnecting all Cars");
                        service.disconnectCars();
                    }
                }
                //Car all
            } else if (topic.endsWith("Car/I")) {
                System.out.println("Global Car");
                service.commandAllCars(json);
                //car
            } else if (topic.contains("Car/")) {
                String[] temp = topic.split("/");
                String car = temp[2];
                System.out.println("Car command for " + car);
                if (service.getListOfCars().containsKey(car)) {
                    service.commandCar(car, json);
                }
                //Invalid Topic
            } else {
                System.out.println("New Topic found");
                System.out.println(topic);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken imdt) {
            System.out.println("Delivery Complete...");
        }
    }
}

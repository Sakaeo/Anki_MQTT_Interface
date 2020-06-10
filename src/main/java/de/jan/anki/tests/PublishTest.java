package de.jan.anki.tests;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.Vehicle;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Some Mqtt publish Test.
 * Strübin Jan, 11.06.20
 */
public class PublishTest {

    public static void main(String[] args) throws MqttException, IOException {
        AnkiConnector anki = new AnkiConnector("Localhost", 5000);
        MqttClient client = new MqttClient("tcp://172.16.133.48:1883", "1616161161hhiija");
        client.connect();

        List<Vehicle> vehicles;
        try {
            vehicles = anki.findVehicles();
            System.out.println("found cars:" + vehicles.size());
            for (Vehicle v : vehicles) {
                JSONObject json = new JSONObject();
                json.put("timestamp", new Timestamp(System.currentTimeMillis()));
                json.put("address", v.getAddress());
                client.publish("AnkiOverdrive/Service/Events/CarDiscovered", new MqttMessage(json.toString().getBytes()));
                System.out.println("found: " + v.getAddress());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

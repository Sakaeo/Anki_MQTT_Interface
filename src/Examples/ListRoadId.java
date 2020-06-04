package de.jan.anki.Examples;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

public class ListRoadId {


    public static void main(String[] args) throws MqttException, IOException {
        String mqttServer = "tcp://192.168.1.243:1883";
        String car = "cec233dec1cb";

        MqttClient client = new MqttClient(mqttServer, "ListRoadIdClient");
        Writer writer = new Writer();

        client.connect();
        client.setCallback(new Handler(writer));

        client.subscribe("Anki/Car/" + car + "/S/PositionInfo");
        System.out.println("Subscribe to PositionInfo of " + car);

        System.out.println("I am going to receive ticks...");
        System.out.println("...press a key to end that.");
        System.in.read();

        writer.close();

        System.out.println("Done.");
        System.exit(0);
    }

    static class Handler implements MqttCallback {
        Writer writer;

        public Handler(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            System.out.println("Connection Lost... mqtt Handler");
            System.exit(0);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            JSONObject json = new JSONObject(new String(message.getPayload()));
            if (json.has("roadPieceId")) {
                writer.write("RoadId: " + json.getInt("roadPieceId") + " ");
            }
            if (json.has("locationId")) {
                writer.write("LocaId: " + json.getInt("locationId") + " ");
            }
            if (json.has("reverse")) {
                writer.write("reverse: " + json.getBoolean("reverse") + "\n");
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    static class Writer {
        FileWriter writer;

        Writer() throws IOException {
            writer = new FileWriter("C:\\Users\\Jan\\Desktop\\RoadId.txt");
        }

        public void write(String msg) throws IOException {
            writer.write(msg);
            System.out.println(msg);
        }

        public void close() throws IOException {
            writer.close();
            System.out.println("closed");
        }
    }
}

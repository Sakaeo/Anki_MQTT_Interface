package de.jan.anki.host;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This Class manages the publish of all description in the /D Topics.
 * Strübin Jan, 11.06.20
 */
public class DescriptionFiller {

    /**
     * Fills the /D Topics with the Description
     * @param server MQTT Broker Ip
     * @param id    Client Name for the MQTT connection
     */
    public static void fill(String server, String id) throws MqttException {
        MQTTPublisher descPublisher = new MQTTPublisher(server,id,"Anki/");

        JSONObject json = new JSONObject();
        json.put("connecting","Boolean");
        json.put("amount","int");
        descPublisher.publish("Host/D/I",json.toString(),true);

        json = new JSONObject();
        json.put("lights","String");
        json.put("speed","long");
        json.put("acceleration","long");
        json.put("lane","long");
        json.put("battery","Boolean");
        json.put("version","Boolean");
        json.put("uTurn","Boolean");
        JSONObject subSubJson = new JSONObject();
        subSubJson.put("expectedRoadPieces","int");
        JSONArray array = new JSONArray();
        array.put("int");
        array.put("int");
        array.put("int");
        array.put("...");
        JSONObject subJson = new JSONObject();
        subJson.put("setTrack",array);
        subJson.put("scanTrack",subSubJson);
        json.put("trackMode",subJson);
        descPublisher.publish("Car/D/I",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("Car","String");
        descPublisher.publish("Host/D/E/CarDiscovered",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("msg","String");
        descPublisher.publish("Host/D/E/Error",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("value","Boolean");
        descPublisher.publish("Host/D/S/HostStatus",json.toString(),true);

        array = new JSONArray();
        array.put("CarId");
        array.put("CarId");
        array.put("CarId");
        array.put("...");
        descPublisher.publish("Host/D/S/Cars",array.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        descPublisher.publish("Car/D/E/Lane/Reached",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        descPublisher.publish("Car/D/E/Speed/Reached",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        descPublisher.publish("Car/D/E/LaneError",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        descPublisher.publish("Car/D/E/Delocalized",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        descPublisher.publish("Car/D/S/DiscoveryTime",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("online","Boolean");
        json.put("charging","Boolean");
        json.put("onTrack","Boolean");
        descPublisher.publish("Car/D/S/CarStatus",json.toString(),true);

        json = new JSONObject();
        json.put("address","String");
        json.put("identifier","int");
        json.put("model","String");
        json.put("modelId","int");
        json.put("productId","int");
        descPublisher.publish("Car/D/S/Information",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("locationId","int");
        json.put("roadPieceId","int");
        json.put("reverse","Boolean");
        json.put("lane","float");
        json.put("speed","int");
        descPublisher.publish("Car/D/S/PositionInfo",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("drivingDirection","int");
        json.put("isExiting","Boolean");
        descPublisher.publish("Car/D/S/IntersectionInfo",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("roadPieceId","int");
        json.put("prevRoadPieceId","int");
        json.put("lane","float");
        descPublisher.publish("Car/D/S/TransitionInfo",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("value","float");
        descPublisher.publish("Car/D/S/Lane/Actual",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("value","long");
        descPublisher.publish("Car/D/S/Lane/Desired",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("value","int");
        descPublisher.publish("Car/D/S/Speed/Actual",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("value","long");
        descPublisher.publish("Car/D/S/Speed/Desired",json.toString(),true);

        array = new JSONArray();
        array.put("RoadPieceId");
        array.put("RoadPieceId");
        array.put("RoadPieceId");
        array.put("...");
        descPublisher.publish("Car/D/S/Track",array.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("value","int");
        descPublisher.publish("Car/D/S/BatteryLevel",json.toString(),true);

        json = new JSONObject();
        json.put("timestamp","Timestamp");
        json.put("value","int");
        descPublisher.publish("Car/D/S/Version",json.toString(),true);

        descPublisher.disconnect();
    }
}

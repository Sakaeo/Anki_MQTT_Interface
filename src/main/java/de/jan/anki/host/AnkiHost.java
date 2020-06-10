package de.jan.anki.host;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.*;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * This class manages the communication between the Node.js Bluetooth Gateway and JSON commands from the Mqtt Broker.
 * Strübin Jan, 11.06.20
 */
public class AnkiHost {
    private final String mqttServer;
    private final AnkiConnector anki;
    private final MQTTPublisher hostPublisher;

    private final Map<String, Vehicle> listOfCars = new HashMap<>();
    private final Map<Vehicle, Map<String, Object>> valuesOfCars = new HashMap<>();
    private final Map<String, MQTTPublisher> listOfPublishers = new HashMap<>();

    /**
     * Constructor for a new Anki Host object
     *
     * @param ankiServer    Ip of the Node.js Bluetooth Gateway
     * @param mqttServer    Ip of the Mqtt Broker
     * @param hostPublisher The Mqtt Publisher for this Anki Host instance
     */
    public AnkiHost(String ankiServer, String mqttServer, MQTTPublisher hostPublisher) throws IOException {
        this.mqttServer = mqttServer;
        this.hostPublisher = hostPublisher;
        this.anki = new AnkiConnector(ankiServer, 5000);

        JSONArray jsonArray = new JSONArray(listOfCars.keySet().toArray());
        hostPublisher.publish("S/Cars", jsonArray.toString(), true);
    }

    /**
     * Starts the scanning for Anki Overdrive Cars. There are 2 Options for Scanning:
     * With expected amount of cars and without. While scanning for cars without an expected amount only up to 4 cars can be found in each scan.
     *
     * @param payload Is empty or contains the amount of expected cars as an int
     */
    public void scanCar(JSONObject payload) throws MqttException, InterruptedException {

        // Expected amount Option, stops after 3 tries without new cars or if an empty scan is done
        if (payload.has("amount")) {
            int expectedCars = payload.getInt("amount");
            boolean running = true;
            int tries = 0;
            while (running) {
                List<Vehicle> vehicles = anki.findVehicles();
                if (expectedCars <= listOfCars.size()) {
                    running = false;
                }
                if (vehicles.isEmpty() && running || tries > 3) {
                    running = false;
                    JSONObject json = new JSONObject();
                    json.put("timestamp", new Timestamp(System.currentTimeMillis()));
                    json.put("msg", "Found only " + listOfCars.size() + " out of " + expectedCars + " cars");
                    hostPublisher.publish("E/Error", json.toString());
                }
                for (Vehicle v : vehicles) {
                    addCar(v);
                }
                tries++;
            }
            // Just one scan, can find up to 4 Cars or sometime even less
        } else {
            List<Vehicle> vehicles = anki.findVehicles();
            for (Vehicle v : vehicles) {
                addCar(v);
            }
        }
        System.out.println("now " + listOfCars.size() + " cars in the List");
        JSONArray jsonArray = new JSONArray(listOfCars.keySet().toArray());
        hostPublisher.publish("S/Cars", jsonArray.toString(), true);
    }

    /**
     * Checks if a car is not charging and ads them in to the data map, creates a car Mqtt publisher and publishes the car status and information.
     * Each car gets pinged all 5 seconds and the status is updated. If the ping is unsuccessful the car gets removed.
     *
     * @param v The car that is to be added
     */
    private void addCar(Vehicle v) throws MqttException {
        // Creates new Mqtt publisher for the vehicle
        MQTTPublisher carPublisher = new MQTTPublisher(mqttServer, "Car/" + v.getAddress() + "/Publisher", "Anki/Car/" + v.getAddress() + "/");

        // Ads the car to the data maps
        listOfPublishers.put(v.getAddress(), carPublisher);
        listOfCars.put(v.getAddress(), v);
        valuesOfCars.put(v, new HashMap<>());

        // Car discovered event publish
        JSONObject json = new JSONObject();
        json.put("timestamp", new Timestamp(System.currentTimeMillis()));
        carPublisher.publish("S/DiscoveryTime", json.toString(),true);
        json.put("Car", v.getAddress());
        hostPublisher.publish("E/CarDiscovered", json.toString());

        // Connection the car and adding of listeners
        v.connect();
        addListener(v, carPublisher);

        // Initial Message and firs Ping to get the online status
        v.sendMessage(new SdkModeMessage());
        v.sendMessage(new PingRequestMessage());

        // Pings the car all 5 Seconds and updates the online status
        Timer carUpdater = new Timer();
        carUpdater.schedule(new CarPing(carPublisher, v, this, carUpdater), 0, 5000);

        // Publish of car information
        json = new JSONObject();
        json.put("address", v.getAddress());
        json.put("identifier", v.getAdvertisement().getIdentifier());
        json.put("model", v.getAdvertisement().getModel());
        json.put("modelId", v.getAdvertisement().getModelId());
        json.put("productId", v.getAdvertisement().getProductId());
        carPublisher.publish("S/Information", json.toString(), true);
    }

    /**
     * Updates online status, removes car from the data maps and disconnects the car Mqtt publisher.
     *
     * @param v The Car to be removed
     */
    public void removeCar(Vehicle v) {
        MQTTPublisher carPublisher = listOfPublishers.get(v.getAddress());

        JSONObject json = new JSONObject();
        json.put("timestamp", new Timestamp(System.currentTimeMillis()));
        json.put("online", false);
        carPublisher.publish("S/CarStatus", json.toString(), true);

        listOfCars.remove(v.getAddress());
        listOfPublishers.remove(v.getAddress());
        valuesOfCars.remove(v);

        carPublisher.disconnect();

        JSONArray jsonArray = new JSONArray(listOfCars.keySet().toArray());
        hostPublisher.publish("S/Cars", jsonArray.toString(), true);
    }

    /**
     * Disconnects all Cars
     */
    public void disconnectCars() throws InterruptedException {
        Map<String, Vehicle> temp = new HashMap<>(listOfCars);
        for (Vehicle v : temp.values()) {
            stopCar(v);
            v.disconnect();
            System.out.println(v.getAddress() + " disconnected");
        }
    }

    /**
     * Stars the scanning of road pieces. Adds two new Listener for recording of road piece id and traveled amount
     *
     * @param v Car that is scanning the road
     */
    private void scanRoadStart(Vehicle v) {
        Map<String, Object> values = valuesOfCars.get(v);
        values.put("track", new ArrayList<Integer>());
        values.put("roadPiecesScanned", 0);
        values.put("newRoadPiece", true);

        // Records all traveled road piece Ids
        RoadIdRecorder roadId = new RoadIdRecorder(v);
        v.addMessageListener(LocalizationPositionUpdateMessage.class, roadId);

        // Counts the traveled road pieces
        RoadPieceCounter roadCount = new RoadPieceCounter(v);
        v.addMessageListener(LocalizationTransitionUpdateMessage.class, roadCount);
        values.put("roadCountListener", roadCount);

        // Car starts driving
        v.sendMessage(new SetSpeedMessage(300, 100));
        System.out.println("Start Scan");
    }

    /**
     * Stops the scanning of the road pieces and publishes the result
     * Is invoked by the RoadIdRecoder Listener
     *
     * @param v      The car that is scanning
     * @param roadId The RoadIdRecorder that invoked this method
     */
    private void scanRoadStop(Vehicle v, RoadIdRecorder roadId) {
        Map<String, Object> values = valuesOfCars.get(v);

        v.removeMessageListener(LocalizationPositionUpdateMessage.class, roadId);
        v.removeMessageListener(LocalizationTransitionUpdateMessage.class, (RoadPieceCounter) values.get("roadCountListener"));

        values.remove("roadPiecesToScan");

        @SuppressWarnings("unchecked")
        JSONArray json = new JSONArray(((ArrayList<Integer>) values.get("track")).toArray());
        listOfPublishers.get(v.getAddress()).publish("S/Track", json.toString(), true);
        System.out.println("Stop Scan");
    }

    /**
     * Sets speed to 0 with a negative acceleration
     *
     * @param v The car that is stopping
     */
    public void stopCar(Vehicle v) {
        v.sendMessage(new SetSpeedMessage(0, -500));
    }

    /**
     * Sets the lighting options of a specific car. Only red and green light are implemented. Additional lighting options can be added later.
     *
     * @param v Target car for the lighting change
     * @param s Key String for lighting change. "red","green" are implemented
     */
    private void carLight(Vehicle v, String s) {
        if (s.equals("red")) {
            LightsPatternMessage.LightConfig lc = new LightsPatternMessage.LightConfig(LightsPatternMessage.LightChannel.ENGINE_RED, LightsPatternMessage.LightEffect.STEADY, 100, 0, 0);
            LightsPatternMessage lpm = new LightsPatternMessage();
            lpm.add(lc);
            v.sendMessage(lpm);
        } else if (s.equals("green")) {
            LightsPatternMessage.LightConfig lc = new LightsPatternMessage.LightConfig(LightsPatternMessage.LightChannel.ENGINE_GREEN, LightsPatternMessage.LightEffect.STEADY, 100, 0, 0);
            LightsPatternMessage lpm = new LightsPatternMessage();
            lpm.add(lc);
            v.sendMessage(lpm);
        } else {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("msg", "Unknown Light value: " + s);
            listOfPublishers.get(v.getAddress()).publish("E/Error", json.toString());
            System.out.println("Unknown Light value: " + s);
        }
    }

    /**
     * Adds all the different listener do a car. They manage the respond messages from the Node.js Gateway
     *
     * @param v            Car that has all listener added
     * @param carPublisher Mqtt publisher for the specific car
     */
    private void addListener(Vehicle v, MQTTPublisher carPublisher) {
        // Position Handler
        LocalizationPositionHandler lph = new LocalizationPositionHandler(v, carPublisher);
        v.addMessageListener(LocalizationPositionUpdateMessage.class, lph);

        //Intersection Handler
        LocalizationIntersectionHandler lih = new LocalizationIntersectionHandler(v, carPublisher);
        v.addMessageListener(LocalizationIntersectionUpdateMessage.class, lih);

        //Transition Handler
        LocalizationTransitionHandler lth = new LocalizationTransitionHandler(v, carPublisher);
        v.addMessageListener(LocalizationTransitionUpdateMessage.class, lth);

        //VersionRequest Handler
        VersionResponseHandler vrh = new VersionResponseHandler(v, carPublisher);
        v.addMessageListener(VersionResponseMessage.class, vrh);

        //BatteryLever Handler
        BatteryLevelResponseHandler brh = new BatteryLevelResponseHandler(v, carPublisher);
        v.addMessageListener(BatteryLevelResponseMessage.class, brh);

        //Ping Handler
        PingResponseHandler prh = new PingResponseHandler(v, carPublisher);
        v.addMessageListener(PingResponseMessage.class, prh);

        //Event Handler for LaneError
        OffsetFromRoadCenterUpdateMessageHandler och = new OffsetFromRoadCenterUpdateMessageHandler(v, carPublisher);
        v.addMessageListener(OffsetFromRoadCenterUpdateMessage.class, och);

        //Event Handler for Delocalized
        VehicleDelocalizedMessageHandler vdh = new VehicleDelocalizedMessageHandler(v, carPublisher);
        v.addMessageListener(VehicleDelocalizedMessage.class, vdh);

        // Info Handler for onTrack or charging Status
        VehicleInfoMessageHandler vih = new VehicleInfoMessageHandler(v, carPublisher);
        v.addMessageListener(VehicleInfoMessage.class, vih);
    }

    /**
     * Sends the payload to all cars in the listOfCars data map
     *
     * @param payload Message that is forwarded
     */
    public void commandAllCars(JSONObject payload) {
        for (Vehicle v : listOfCars.values()) {
            commandCar(v, payload);
        }
    }

    /**
     * Searches for the specific Vehicle object for the address string of a car. Then invokes the overloaded method with the same Payload.
     *
     * @param address Car address as a String
     * @param payload JSON object containing the car intend
     */
    public void commandCar(String address, JSONObject payload) {
        commandCar(listOfCars.get(address), payload);
    }

    /**
     * Goes through the payload and searches for key strings. If the a correct intent is found the  corresponding message is send to the Node.js Gateway.
     *
     * @param v       Car that receives the intent
     * @param payload JSON object that contains the intent
     */
    public void commandCar(Vehicle v, JSONObject payload) {
        MQTTPublisher carPublisher = listOfPublishers.get(v.getAddress());
        Map<String, Object> values = valuesOfCars.get(v);

        // lights
        if (payload.has("lights")) {
            carLight(v, payload.getString("lights"));
        }
        // speed
        if (payload.has("speed")) {
            long speed = payload.getLong("speed");
            if (payload.has("acceleration")) {
                long acceleration = payload.getLong("speed");
                v.sendMessage(new SetSpeedMessage((int) speed, (int) acceleration));
            } else if (speed == 0) {
                stopCar(v);
            } else {
                v.sendMessage(new SetSpeedMessage((int) speed, 100));
            }
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("value", speed);
            carPublisher.publish("S/Speed/Desired", json.toString(), true);

            values.put("speedDes", speed);
        }
        // lane
        if (payload.has("lane")) {
            long lane = payload.getLong("lane");
            if (lane > -70 && lane < 70) {
                if (values.containsKey("lineNow")) {
                    if ((long) values.get("lineNow") < -70 || (long) values.get("lineNow") > 70) {
                        values.put("lineNow", (long) 0);
                    }
                    v.sendMessage(new SetOffsetFromRoadCenterMessage((long) values.get("lineNow")));
                }
                v.sendMessage(new ChangeLaneMessage((int) lane, 500, 100));

                JSONObject json = new JSONObject();
                json.put("timestamp", new Timestamp(System.currentTimeMillis()));
                json.put("value", lane);
                carPublisher.publish("S/Lane/Desired", json.toString(), true);

                values.put("lineDes", lane);
            } else {
                JSONObject json = new JSONObject();
                json.put("timestamp", new Timestamp(System.currentTimeMillis()));
                json.put("msg", "Lane out of range (-70,70)");
                listOfPublishers.get(v.getAddress()).publish("E/Error", json.toString());
                System.out.println("lane out if range (-70,70)");
            }
        }
        // Set and Scan track
        if (payload.has("trackMode")) {
            payload = payload.getJSONObject("trackMode");
            if (payload.has("setTrack") && payload.has("scanTrack")) {
                JSONObject json = new JSONObject();
                json.put("timestamp", new Timestamp(System.currentTimeMillis()));
                json.put("msg", "Only 1 intent for Track mode");
                listOfPublishers.get(v.getAddress()).publish("E/Error", json.toString());
                System.out.println("Only 1 Key pls");
            } else if (payload.has("setTrack")) {
                values.put("track", payload.getJSONArray("setTrack"));
                JSONArray json = payload.getJSONArray("setTrack");
                carPublisher.publish("S/Track", json.toString());
            } else if (payload.has("scanTrack")) {
                payload = payload.getJSONObject("scanTrack");
                if (payload.has("expectedRoadPieces")) {
                    values.put("roadPiecesToScan", payload.getInt("expectedRoadPieces"));
                }
                scanRoadStart(v);
            }
        }
        // battery
        if (payload.has("battery")) {
            if (payload.getBoolean("battery")) {
                v.sendMessage(new BatteryLevelRequestMessage());
            }
        }
        // version
        if (payload.has("version")) {
            if (payload.getBoolean("version")) {
                v.sendMessage(new VersionRequestMessage());
            }
        }
        // uTurn
        if (payload.has("uTurn")) {
            if (payload.getBoolean("uTurn")) {
                v.sendMessage(new TurnMessage(3, 1));
            }
        }
    }

    /**
     * Returns all Cars that are connected
     *
     * @return Map with address Strings and corresponding Vehicle objects
     */
    public Map<String, Vehicle> getListOfCars() {
        return listOfCars;
    }

    /**
     * Checks if a is online or not and returns the corresponding boolean
     *
     * @param v Checks the online status of this car
     * @return Returns the online status af a car as a boolean
     */
    public boolean getCarStatus(Vehicle v) {
        if (valuesOfCars.get(v).containsKey("online")) {
            return (boolean) valuesOfCars.get(v).get("online");
        } else {
            return false;
        }
    }

    /**
     * Sets the online status of a car
     *
     * @param v      Sets the online status of this car
     * @param status Represents the online status
     */
    public void setCarStatus(Vehicle v, boolean status) {
        valuesOfCars.get(v).put("online", status);
    }

    /*
     *
     * Nested Classes below
     *
     */

    /*
     * Receives a message from the Node.js Gateway and publishes the message.
     * Sends the battery level of a car.
     */
    private static class BatteryLevelResponseHandler implements MessageListener<BatteryLevelResponseMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public BatteryLevelResponseHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(BatteryLevelResponseMessage m) {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("value", m.getBatteryLevel());
            carPublisher.publish("S/BatteryLevel", json.toString(), true);
        }
    }

    /*
     * Receives a message from the Node.js Gateway and publishes the message.
     * Sends the speed and lane of a car and saves it to the data maps.
     */
    private class LocalizationPositionHandler implements MessageListener<LocalizationPositionUpdateMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public LocalizationPositionHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(LocalizationPositionUpdateMessage m) {
            Map<String, Object> values = valuesOfCars.get(v);
            //Speed
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("value", m.getSpeed());
            carPublisher.publish("S/Speed/Actual", json.toString(), true);
            if (values.containsKey("speedDes") && Math.abs((long) m.getSpeed() - (long) values.get("speedDes"))<5) {
                values.remove("speedDes");
                json = new JSONObject();
                json.put("timestamp", new Timestamp((System.currentTimeMillis())));
                carPublisher.publish("E/Speed/Reached", json.toString());
            }
            //Lane
            if (m.getOffsetFromRoadCenter() < -70 || m.getOffsetFromRoadCenter() > 70) {
                v.sendMessage(new SetOffsetFromRoadCenterMessage(1));
                v.sendMessage(new ChangeLaneMessage(0, 500, 100));
            }

            json = new JSONObject();
            json.put("timestamp", new Timestamp((System.currentTimeMillis())));
            json.put("value", m.getOffsetFromRoadCenter());
            carPublisher.publish("S/Lane/Actual", json.toString(), true);

            long temp = 80;
            if (values.containsKey("lineNow")) {
                temp = (long) values.get("lineNow");
            }
            values.put("lineNow", (long) m.getOffsetFromRoadCenter());

            if (values.containsKey("lineDes") && Math.abs((long) m.getOffsetFromRoadCenter() - (long) values.get("lineDes")) < 2.5) {
                values.remove("lineDes");

                json = new JSONObject();
                json.put("timestamp", new Timestamp((System.currentTimeMillis())));
                carPublisher.publish("E/Lane/Reached", json.toString());
            }
            if (temp == (long) values.get("lineNow") && values.containsKey("lineDes")) {
                v.sendMessage(new ChangeLaneMessage((long) values.get("lineDes"), 500, 100));
            }

            //LocationInfo
            json = new JSONObject();
            json.put("timestamp", new Timestamp((System.currentTimeMillis())));
            json.put("locationId", m.getLocationId());
            json.put("roadPieceId", m.getRoadPieceId());
            json.put("reverse", m.isParsedReverse());
            json.put("lane", m.getOffsetFromRoadCenter());
            json.put("speed", m.getSpeed());
            carPublisher.publish("S/PositionInfo", json.toString(), true);
        }
    }

    /*
     * Receives a message from the Node.js Gateway and publishes the message.
     * Sends information about a intersection if the car passes over it.
     */
    private static class LocalizationIntersectionHandler implements MessageListener<LocalizationIntersectionUpdateMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public LocalizationIntersectionHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(LocalizationIntersectionUpdateMessage m) {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("drivingDirection", m.getDrivingDirection());
            json.put("isExiting", m.isExiting());
            carPublisher.publish("S/IntersectionInfo", json.toString(), true);
        }
    }

    /*
     * Receives a message from the Node.js Gateway and publishes the message.
     * Sends information about a road piece if the car passes on to a new one.
     */
    private static class LocalizationTransitionHandler implements MessageListener<LocalizationTransitionUpdateMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public LocalizationTransitionHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(LocalizationTransitionUpdateMessage m) {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("roadPieceId", m.getRoadPieceId());
            json.put("prevRoadPieceId", m.getPrevRoadPieceId());
            json.put("lane", m.getOffsetFromRoadCenter());
            carPublisher.publish("S/TransitionInfo", json.toString(), true);
        }
    }

    /*
     * Receives a message from the Node.js Gateway and publishes the message.
     * Sends the version of a car.
     */
    private static class VersionResponseHandler implements MessageListener<VersionResponseMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public VersionResponseHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(VersionResponseMessage m) {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("value", m.getVersion());
            carPublisher.publish("S/Version", json.toString(), true);
        }
    }

    /*
     * Receives a message from the Node.js Gateway.
     * Sets the status of a car to online if a ping is answered.
     */
    private class PingResponseHandler implements MessageListener<PingResponseMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public PingResponseHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(PingResponseMessage message) {
            valuesOfCars.get(v).put("online", true);
        }
    }


    /*
     * Manages the scanning on a road map and safes the road piece id to the data map.
     * Invokes scanRoadStop() if a Start road piece is found or the expected number of road pieces is found
     */
    private class RoadIdRecorder implements MessageListener<LocalizationPositionUpdateMessage> {
        Vehicle v;

        public RoadIdRecorder(Vehicle v) {
            this.v = v;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void messageReceived(LocalizationPositionUpdateMessage m) {
            Map<String, Object> values = valuesOfCars.get(v);

            if (!values.containsKey("roadPiecesToScan") && m.getRoadPieceId() == 34) {
                ((ArrayList<Integer>) values.get("track")).add(m.getRoadPieceId());
                stopCar(v);
                scanRoadStop(v, this);
            } else if (values.get("roadPiecesScanned") == values.get("roadPiecesToScan")) {
                stopCar(v);
                scanRoadStop(v, this);
            }
            if ((boolean) values.get("newRoadPiece")) {
                ((ArrayList<Integer>) values.get("track")).add(m.getRoadPieceId());
                values.put("newRoadPiece", false);
            }
        }
    }

    /*
     * Manages the scanning on a road map and counts road pieces.
     */
    private class RoadPieceCounter implements MessageListener<LocalizationTransitionUpdateMessage> {
        Vehicle v;

        public RoadPieceCounter(Vehicle v) {
            this.v = v;
        }

        @Override
        public void messageReceived(LocalizationTransitionUpdateMessage m) {
            valuesOfCars.get(v).put("roadPiecesScanned", ((Integer) valuesOfCars.get(v).get("roadPiecesScanned")) + 1);
            valuesOfCars.get(v).put("newRoadPiece", true);
            System.out.println("Scanned: " + valuesOfCars.get(v).get("roadPiecesScanned"));
        }
    }

    /*
     * Receives a message from the Node.js Gateway.
     * Pushes a LaneError Message to the Mqtt Broker.
     */
    private static class OffsetFromRoadCenterUpdateMessageHandler implements MessageListener<OffsetFromRoadCenterUpdateMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public OffsetFromRoadCenterUpdateMessageHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(OffsetFromRoadCenterUpdateMessage m) {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            carPublisher.publish("E/LaneError", json.toString());
        }
    }

    /*
     * Receives a message from the Node.js Gateway.
     * Pushes a Delocalized Message to the Mqtt Broker.
     */
    private static class VehicleDelocalizedMessageHandler implements MessageListener<VehicleDelocalizedMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public VehicleDelocalizedMessageHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(VehicleDelocalizedMessage m) {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            carPublisher.publish("E/Delocalized", json.toString());
        }
    }

    /*
     * Receives a message from the Node.js Gateway.
     * Sets the status of a car to charging if its charging and sets onTrack if the car is on track.
     */
    private static class VehicleInfoMessageHandler implements MessageListener<VehicleInfoMessage> {
        Vehicle v;
        MQTTPublisher carPublisher;

        public VehicleInfoMessageHandler(Vehicle v, MQTTPublisher carPublisher) {
            this.v = v;
            this.carPublisher = carPublisher;
        }

        @Override
        public void messageReceived(VehicleInfoMessage m) {
            v.getAdvertisement().setOnTrack(m.isOnTrack());
            v.getAdvertisement().setCharging(m.isCharging());
        }
    }
}
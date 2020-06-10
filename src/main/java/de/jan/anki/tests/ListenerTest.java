package de.jan.anki.tests;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.*;

import java.io.IOException;
import java.util.List;

/**
 * Some listener Test.
 * Strübin Jan, 11.06.20
 */
public class ListenerTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Launching connector...");
        AnkiConnector anki = new AnkiConnector("169.254.28.205", 5000);
        System.out.print("...looking for cars...");
        List<Vehicle> vehicles = anki.findVehicles();

        String testCar = "d205e";

        if (vehicles.isEmpty()) {
            System.out.println(" NO CARS FOUND. I guess that means we're done.");

        } else {
            System.out.println(" FOUND " + vehicles.size() + " CARS! They are:");

            Vehicle v = vehicles.get(0);
            for (Vehicle tmp : vehicles) {
                System.out.println(tmp.getAddress());
                if (tmp.getAddress().contains(testCar)) {
                    v = tmp;
                }
            }
            if (!v.getAddress().contains(testCar)) {
                System.out.println("Test car not found");
                anki.close();
                System.exit(0);
            }

            System.out.println("\nConnecting to " + v + " @ " + v.getAddress());
            v.connect();
            System.out.print("   Connected. Setting SDK mode...");   //always set the SDK mode FIRST!
            v.sendMessage(new SdkModeMessage());
            System.out.println("   SDK Mode set.");

            BatteryLevelRequestMessageHandler1 h1 = new BatteryLevelRequestMessageHandler1();
//            v.addMessageListener(BatteryLevelRequestMessage.class, h1);

            BatteryLevelResponseMessageHandler2 h2 = new BatteryLevelResponseMessageHandler2();
//            v.addMessageListener(BatteryLevelResponseMessage.class, h2);

            CancelLaneChangeMessageHandler3 h3 = new CancelLaneChangeMessageHandler3();
//            v.addMessageListener(CancelLaneChangeMessage.class, h3);

            ChangeLaneMessageHandler4 h4 = new ChangeLaneMessageHandler4();
//            v.addMessageListener(ChangeLaneMessage.class, h4);

            LightsPatternMessageHandler5 h5 = new LightsPatternMessageHandler5();
//            v.addMessageListener(LightsPatternMessage.class, h5);

            LocalizationIntersectionUpdateMessageHandler6 h6 = new LocalizationIntersectionUpdateMessageHandler6();
//            v.addMessageListener(LocalizationIntersectionUpdateMessage.class, h6);

            LocalizationPositionUpdateMessageHandler7 h7 = new LocalizationPositionUpdateMessageHandler7();
//            v.addMessageListener(LocalizationPositionUpdateMessage.class, h7);

            LocalizationTransitionUpdateMessageHandler8 h8 = new LocalizationTransitionUpdateMessageHandler8();
            v.addMessageListener(LocalizationTransitionUpdateMessage.class, h8);

            OffsetFromRoadCenterUpdateMessageHandler9 h9 = new OffsetFromRoadCenterUpdateMessageHandler9();
//            v.addMessageListener(OffsetFromRoadCenterUpdateMessage.class, h9);

            PingRequestMessageHandler10 h10 = new PingRequestMessageHandler10();
//            v.addMessageListener(PingRequestMessage.class, h10);

            PingResponseMessageHandler11 h11 = new PingResponseMessageHandler11();
//            v.addMessageListener(PingResponseMessage.class, h11);

            SdkModeMessageHandler12 h12 = new SdkModeMessageHandler12();
//            v.addMessageListener(SdkModeMessage.class, h12);

            SetConfigParamsMessageHandler13 h13 = new SetConfigParamsMessageHandler13();
//            v.addMessageListener(SetConfigParamsMessage.class, h13);

            SetOffsetFromRoadCenterMessageHandler14 h14 = new SetOffsetFromRoadCenterMessageHandler14();
//            v.addMessageListener(SetOffsetFromRoadCenterMessage.class, h14);

            SetSpeedMessageHandler15 h15 = new SetSpeedMessageHandler15();
//            v.addMessageListener(SetSpeedMessage.class, h15);

            TurnMessageHandler16 h16 = new TurnMessageHandler16();
//            v.addMessageListener(TurnMessage.class, h16);

            VehicleDelocalizedMessageHandler17 h17 = new VehicleDelocalizedMessageHandler17();
//            v.addMessageListener(VehicleDelocalizedMessage.class, h17);

            VehicleInfoMessageHandler18 h18 = new VehicleInfoMessageHandler18();
//            v.addMessageListener(VehicleInfoMessage.class, h18);

            VersionRequestMessageHandler19 h19 = new VersionRequestMessageHandler19();
//            v.addMessageListener(VersionRequestMessage.class, h19);

            VersionResponseMessageHandler20 h20 = new VersionResponseMessageHandler20();
//            v.addMessageListener(VersionResponseMessage.class, h20);

            v.sendMessage(new SetSpeedMessage(500,100));

            v.sendMessage(new SetOffsetFromRoadCenterMessage(0));
            v.sendMessage(new ChangeLaneMessage(50,200,100));

            Thread.sleep(5000);
            System.out.println("YEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEET");

            v.sendMessage(new TurnMessage(3,1));
            v.sendMessage(new SetOffsetFromRoadCenterMessage(50));
            v.sendMessage(new ChangeLaneMessage(0,200,100));

            Thread.sleep(5000);

            /*
            System.out.println("Enter to cancel");
            System.out.println(System.in.read());
            */

            v.sendMessage(new SetSpeedMessage(0,-200));
            Thread.sleep(1000);
            v.disconnect();
            System.out.println("not disconnected from " + v + "\n");
        }

        anki.close();
        System.exit(0);
    }


    private static class BatteryLevelRequestMessageHandler1 implements MessageListener<BatteryLevelRequestMessage> {

        @Override
        public void messageReceived(BatteryLevelRequestMessage message) {
            System.out.println("Handler 1");
        }
    }

    private static class BatteryLevelResponseMessageHandler2 implements MessageListener<BatteryLevelResponseMessage> {

        @Override
        public void messageReceived(BatteryLevelResponseMessage message) {
            System.out.println("Handler 2");
        }
    }

    private static class CancelLaneChangeMessageHandler3 implements MessageListener<CancelLaneChangeMessage> {

        @Override
        public void messageReceived(CancelLaneChangeMessage message) {
            System.out.println("Handler 3");
        }
    }

    private static class ChangeLaneMessageHandler4 implements MessageListener<ChangeLaneMessage> {

        @Override
        public void messageReceived(ChangeLaneMessage message) {
            System.out.println("Handler 4");
        }
    }

    private static class LightsPatternMessageHandler5 implements MessageListener<LightsPatternMessage> {

        @Override
        public void messageReceived(LightsPatternMessage message) {
            System.out.println("Handler 5");
        }
    }

    private static class LocalizationIntersectionUpdateMessageHandler6 implements MessageListener<LocalizationIntersectionUpdateMessage> {

        @Override
        public void messageReceived(LocalizationIntersectionUpdateMessage message) {
            System.out.println("Handler 6");
            System.out.println("getRoadPieceId: " + message.getRoadPieceId());
            System.out.println("getOffsetFromRoadCenter: " + message.getOffsetFromRoadCenter());
            System.out.println("getDrivingDirection: " + message.getDrivingDirection());
            System.out.println("getIntersectionCode: " + message.getIntersectionCode());
            System.out.println("getIntersectionTurn: " + message.getIntersectionTurn());
            System.out.println("isExiting: " + message.isExiting());


        }
    }

    private static class LocalizationPositionUpdateMessageHandler7 implements MessageListener<LocalizationPositionUpdateMessage> {

        @Override
        public void messageReceived(LocalizationPositionUpdateMessage message) {
            System.out.println("Handler 7");
            System.out.println("getLocationId: "+ message.getLocationId());
            System.out.println("getRoadPieceId: "+ message.getRoadPieceId());
            System.out.println("getOffsetFromRoadCenter: "+ message.getOffsetFromRoadCenter());
            System.out.println("getSpeed: "+ message.getSpeed());
            System.out.println("getParsingFlags: "+ message.getParsingFlags());
            System.out.println("getLastReceivedLaneChangeId: "+ message.getLastReceivedLaneChangeId());
            System.out.println("getLastExecutedLaneChangeId: "+ message.getLastExecutedLaneChangeId());
            System.out.println("getLastDesiredHorizontalSpeed: "+ message.getLastDesiredHorizontalSpeed());
            System.out.println("getLastDesiredSpeed: "+ message.getLastDesiredSpeed());
            System.out.println("isParsedReverse: "+ message.isParsedReverse());
        }
    }

    private static class LocalizationTransitionUpdateMessageHandler8 implements MessageListener<LocalizationTransitionUpdateMessage> {

        @Override
        public void messageReceived(LocalizationTransitionUpdateMessage message) {
            System.out.println("Handler 8");
            System.out.println("getRoadPieceId: "+ message.getRoadPieceId());
            System.out.println("getPrevRoadPieceId: "+ message.getPrevRoadPieceId());
            System.out.println("getOffsetFromRoadCenter: "+ message.getOffsetFromRoadCenter());
            System.out.println("getDrivingDirection: "+ message.getDrivingDirection());
            System.out.println("getLastReceivedLaneChangeId: "+ message.getLastReceivedLaneChangeId());
            System.out.println("getLastExecutedLaneChangeId: "+ message.getLastExecutedLaneChangeId());
            System.out.println("getLastDesiredHorizontalSpeed: "+ message.getLastDesiredHorizontalSpeed());
            System.out.println("getLastDesiredSpeed: "+ message.getLastDesiredSpeed());
            System.out.println("getUphillCounter: "+ message.getUphillCounter());
            System.out.println("getDownhillCounter: "+ message.getDownhillCounter());
            System.out.println("getLeftWheelDistance: "+ message.getLeftWheelDistance());
            System.out.println("getRightWheelDistance: "+ message.getRightWheelDistance());
        }
    }

    private static class OffsetFromRoadCenterUpdateMessageHandler9 implements MessageListener<OffsetFromRoadCenterUpdateMessage> {

        @Override
        public void messageReceived(OffsetFromRoadCenterUpdateMessage message) {
            System.out.println("Handler 9");
        }
    }

    private static class PingRequestMessageHandler10 implements MessageListener<PingRequestMessage> {

        @Override
        public void messageReceived(PingRequestMessage message) {
            System.out.println("Handler 10");
        }
    }

    private static class PingResponseMessageHandler11 implements MessageListener<PingResponseMessage> {

        @Override
        public void messageReceived(PingResponseMessage message) {
            System.out.println("Handler 11");
        }
    }

    private static class SdkModeMessageHandler12 implements MessageListener<SdkModeMessage> {

        @Override
        public void messageReceived(SdkModeMessage message) {
            System.out.println("Handler 12");
        }
    }

    private static class SetConfigParamsMessageHandler13 implements MessageListener<SetConfigParamsMessage> {

        @Override
        public void messageReceived(SetConfigParamsMessage message) {
            System.out.println("Handler 13");
        }
    }

    private static class SetOffsetFromRoadCenterMessageHandler14 implements MessageListener<SetOffsetFromRoadCenterMessage> {

        @Override
        public void messageReceived(SetOffsetFromRoadCenterMessage message) {
            System.out.println("Handler 14");
        }
    }

    private static class SetSpeedMessageHandler15 implements MessageListener<SetSpeedMessage> {

        @Override
        public void messageReceived(SetSpeedMessage message) {
            System.out.println("Handler 15");
        }
    }

    private static class TurnMessageHandler16 implements MessageListener<TurnMessage> {

        @Override
        public void messageReceived(TurnMessage message) {
            System.out.println("Handler 16");
        }
    }

    private static class VehicleDelocalizedMessageHandler17 implements MessageListener<VehicleDelocalizedMessage> {

        @Override
        public void messageReceived(VehicleDelocalizedMessage message) {
            System.out.println("Handler 17");
        }
    }

    private static class VehicleInfoMessageHandler18 implements MessageListener<VehicleInfoMessage> {

        @Override
        public void messageReceived(VehicleInfoMessage message) {
            System.out.println("Handler 18");
        }
    }

    private static class VersionRequestMessageHandler19 implements MessageListener<VersionRequestMessage> {

        @Override
        public void messageReceived(VersionRequestMessage message) {
            System.out.println("Handler 19");
        }
    }

    private static class VersionResponseMessageHandler20 implements MessageListener<VersionResponseMessage> {

        @Override
        public void messageReceived(VersionResponseMessage message) {
            System.out.println("Handler 20");
        }
    }
}


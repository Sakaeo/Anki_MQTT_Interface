package de.jan.anki.tests;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static de.adesso.anki.messages.LightsPatternMessage.*;

/**
 * First Test kinda from @tenbergen    https://github.com/tenbergen/anki-drive-java.
 * Strübin Jan, 11.06.20
 */
public class MainTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Launching connector...");
        AnkiConnector anki = new AnkiConnector("192.168.1.237", 5000);
        System.out.print("...looking for cars...");
        List<Vehicle> vehicles = anki.findVehicles();

        if (vehicles.isEmpty()) {
            System.out.println(" NO CARS FOUND. I guess that means we're done.");

        } else {
            System.out.println(" FOUND " + vehicles.size() + " CARS! They are:");

            Iterator<Vehicle> iter = vehicles.iterator();
            while (iter.hasNext()) {
                Vehicle v = iter.next();
                System.out.println("   " + v);
                System.out.println("      ID: " + v.getAdvertisement().getIdentifier());
                System.out.println("      Model: " + v.getAdvertisement().getModel());
                System.out.println("      Model ID: " + v.getAdvertisement().getModelId());
                System.out.println("      Product ID: " + v.getAdvertisement().getProductId());
                System.out.println("      Address: " + v.getAddress());
                System.out.println("      Color: " + v.getColor());
                System.out.println("      charging? " + v.getAdvertisement().isCharging());
            }

            System.out.println("\nNow connecting to and doing stuff to your cars.\n\n");

            iter = vehicles.iterator();
            while (iter.hasNext()) {
                Vehicle v = iter.next();
                System.out.println("\nConnecting to " + v + " @ " + v.getAddress());
                v.connect();
                System.out.print("   Connected. Setting SDK mode...");   //always set the SDK mode FIRST!
                v.sendMessage(new SdkModeMessage());
                System.out.println("   SDK Mode set.");

/*
                System.out.println("   Sending asynchronous Battery Level Request. The Response will come in eventually.");
                //we have to set up a response handler first, in order to handle async responses
                BatteryLevelResponseHandler blrh = new BatteryLevelResponseHandler();
                //now we tell the car, who is listenening to the replies
                v.addMessageListener(BatteryLevelResponseMessage.class, blrh);
                //now we can actually send it.
                v.sendMessage(new BatteryLevelRequestMessage());
*/

                System.out.println("   Flashing lights...");
                LightConfig lc = new LightConfig(LightChannel.ENGINE_RED, LightEffect.FADE,
                        100, 10, 0);
                LightsPatternMessage lpm = new LightsPatternMessage();
                lpm.add(lc);
                v.sendMessage(lpm);
/*
                System.out.println("   Setting Speed...");
                v.sendMessage(new SetSpeedMessage(500, 100));

                // Position Handler
                System.out.println("    Position");
                LocalizationPositionHandler lph = new LocalizationPositionHandler();
                //  v.addMessageListener(LocalizationPositionUpdateMessage.class, lph);

                //Intersection Handler
                System.out.println("    Intersection");
                LocalizationIntersectionHandler lih = new LocalizationIntersectionHandler();
                v.addMessageListener(LocalizationIntersectionUpdateMessage.class, lih);

                //Transition Handler
                System.out.println("    Transition");
                LocalizationTransitionHandler lth = new LocalizationTransitionHandler();
                // v.addMessageListener(LocalizationTransitionUpdateMessage.class,lth);

                //VersionRequest Handler
                System.out.println("    Version request");
                VersionResponseHandler vrh = new VersionResponseHandler();
                v.addMessageListener(VersionResponseMessage.class, vrh);
                v.sendMessage(new VersionRequestMessage());


                System.out.print("  Sleeping for 5secs... ");
                Thread.sleep(5000);

                System.out.println("    Change Line to 23.0");
                //v.sendMessage(new SetOffsetFromRoadCenterMessage(23));
                v.sendMessage(new ChangeLaneMessage(23, 500, 100));

                System.out.print("  Sleeping for 5secs... ");
                Thread.sleep(5000);

                System.out.println("    Change Line to -23.0");
                //v.sendMessage(new CancelLaneChangeMessage());
                //v.sendMessage(new SetOffsetFromRoadCenterMessage(-23));
                v.sendMessage(new ChangeLaneMessage(-23, 500, 100));

                System.out.print("  Sleeping for 5secs... ");
                Thread.sleep(5000);
*/

                v.disconnect();
                System.out.println("not disconnected from " + v + "\n");
            }
        }
        anki.close();
        System.exit(0);
    }

    /**
     * Handles the response from the vehicle from the BatteryLevelRequestMessage.
     * We need handler classes because responses from the vehicles are asynchronous.
     */
    private static class BatteryLevelResponseHandler implements MessageListener<BatteryLevelResponseMessage> {
        @Override
        public void messageReceived(BatteryLevelResponseMessage m) {
            System.out.println("   Battery Level is: " + m.getBatteryLevel() + " mV");
        }
    }

    private static class LocalizationPositionHandler implements MessageListener<LocalizationPositionUpdateMessage> {
        @Override
        public void messageReceived(LocalizationPositionUpdateMessage m) {
            System.out.println("" + m.toString() + "");
        }
    }

    private static class LocalizationIntersectionHandler implements MessageListener<LocalizationIntersectionUpdateMessage> {
        @Override
        public void messageReceived(LocalizationIntersectionUpdateMessage m) {
            System.out.println(m.toString());
        }
    }

    private static class LocalizationTransitionHandler implements MessageListener<LocalizationTransitionUpdateMessage> {
        @Override
        public void messageReceived(LocalizationTransitionUpdateMessage m) {
            System.out.println(m.toString());
        }
    }

    private static class VersionResponseHandler implements MessageListener<VersionResponseMessage> {
        @Override
        public void messageReceived(VersionResponseMessage m) {
            System.out.println("Version: " + m.toString());
        }
    }

}
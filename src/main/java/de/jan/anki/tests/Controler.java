package de.jan.anki.tests;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Some controller Test.
 * Strübin Jan, 11.06.20
 */
public class Controler {

    private static float offset;
    private static int speed;
    private static float[] offsets = {-100, -66, -23, 0, 23, 66, 100};

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Launching connector...");
        AnkiConnector anki = new AnkiConnector("Localhost", 5000);
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

                if (v.getAdvertisement().isCharging()) {
                    vehicles.remove(v);
                    System.out.println(v + " is Charging and is removed");
                }
            }

            System.out.println("\nNow connecting to and doing stuff to your cars.\n\n");

            Scanner scanner = new Scanner(System.in);
            iter = vehicles.iterator();
            while (iter.hasNext()) {
                Vehicle v = iter.next();
                System.out.println("\nConnecting to " + v + " @ " + v.getAddress());
                v.connect();
                System.out.print("   Connected. Setting SDK mode...");   //always set the SDK mode FIRST!
                v.sendMessage(new SdkModeMessage());
                System.out.println("   SDK Mode set.");


                System.out.println("    Position");
                LocalizationPositionHandler lph = new LocalizationPositionHandler();
                v.addMessageListener(LocalizationPositionUpdateMessage.class, lph);

                boolean running = true;
                while (running) {
                    System.out.println("w = Start, s = stop, a = change lane left, d = change lane right, q = U-Turn,  e = exit");
                    String input = scanner.nextLine();

                    switch (input) {
                        case "w":
                            v.sendMessage(new SetSpeedMessage(500, 100));
                            break;
                        case "s":
                            v.sendMessage(new SetSpeedMessage(0, -100));
                            break;
                        case "a":
                            v.sendMessage(new SetOffsetFromRoadCenterMessage(offset));
                            changeLane(-1, v);
                            break;
                        case "d":
                            v.sendMessage(new SetOffsetFromRoadCenterMessage(offset));
                            changeLane(1, v);
                            break;
                        case "e":
                            v.sendMessage(new SetSpeedMessage(0, -100));
                            running = false;
                            break;
                        case "q":
                            v.sendMessage(new TurnMessage(3, 1));
                            break;
                    }
                }
                v.disconnect();
                System.out.println("disconnected from " + v + "\n");
            }
        }
        anki.close();
        System.exit(0);
    }

    private static int matchOffset() {
        int result = 0;
        float temp = 100;
        for (int i = 0; i < offsets.length; i++) {
            if (Math.abs(offset - offsets[i]) < temp) {
                temp = offset - offsets[i];
                result = i;
            }
        }
        return result;
    }

    private static void changeLane(int i, Vehicle v) {
        int result = matchOffset() + i;
        if (result > offsets.length || result < 0) {
            System.out.println("Lane change not possible");
        } else {
            v.sendMessage(new ChangeLaneMessage(offsets[result], 500, 100));
        }
    }

    private static class LocalizationPositionHandler implements MessageListener<LocalizationPositionUpdateMessage> {
        @Override
        public void messageReceived(LocalizationPositionUpdateMessage m) {
            Controler.offset = m.getOffsetFromRoadCenter();
            Controler.speed = m.getSpeed();
            System.out.printf("Offset = %f.02, Speed = %d\n", Controler.offset, Controler.speed);
            //System.out.println(m.toString());
        }
    }
}


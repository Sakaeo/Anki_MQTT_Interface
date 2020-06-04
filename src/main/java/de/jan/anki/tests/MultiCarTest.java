package de.jan.anki.tests;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.SdkModeMessage;
import de.adesso.anki.messages.SetSpeedMessage;
import de.adesso.anki.messages.VersionRequestMessage;
import de.adesso.anki.messages.VersionResponseMessage;

import java.io.IOException;
import java.util.List;

public class MultiCarTest {

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println("Launching connector...");
        AnkiConnector anki = new AnkiConnector("Localhost", 5000);
        System.out.print("...looking for cars...");
        List<Vehicle> vehicles = anki.findVehicles();

        if (vehicles.isEmpty()) {
            System.out.println(" NO CARS FOUND. I guess that means we're done.");

        } else {
            System.out.println("found "+vehicles.size()+" cars");

            for (Vehicle v:vehicles){
                System.out.println(v.getAddress());
                System.out.println(v.getAdvertisement().toString());
                System.out.println("----");
            }

            for(Vehicle v:vehicles){
                System.out.println("connect to "+v.getAddress());
                v.connect();
                v.sendMessage(new SdkModeMessage());


                VersionResponseHandler vrh = new VersionResponseHandler();
                v.addMessageListener(VersionResponseMessage.class,vrh);

                v.sendMessage(new VersionRequestMessage());

                v.sendMessage(new SetSpeedMessage(500, 100));
                System.out.println("drive " +v.getAddress());

                System.out.println("2s sleep --------------------------");
                Thread.sleep(2000);
            }

            System.out.println("10s sleep");
            Thread.sleep(10000);

            for (Vehicle v:vehicles){
                System.out.println("Disconnecting "+ v.getAddress());
                v.sendMessage(new SetSpeedMessage(0,-100));
                Thread.sleep(2000);
                v.disconnect();
            }

        }
        System.exit(0);
    }
    private static class VersionResponseHandler implements MessageListener<VersionResponseMessage> {
        @Override
        public void messageReceived(VersionResponseMessage m){
            System.out.println(m.getVersion());
        }
    }
}
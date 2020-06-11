package de.jan.anki.tests;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.*;

import java.io.IOException;
import java.util.List;

import static de.adesso.anki.messages.LightsPatternMessage.*;

/**
 * Some simple Test kinda from @tenbergen    https://github.com/tenbergen/anki-drive-java.
 * Strübin Jan, 11.06.20
 */
public class SimpleTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Launching connector...");
        AnkiConnector anki = new AnkiConnector("192.168.1.237", 5000);
        System.out.print("...looking for cars...");
        List<Vehicle> vehicles = anki.findVehicles();

        if (vehicles.isEmpty()) {
            System.out.println(" NO CARS FOUND. I guess that means we're done.");

        } else {
            System.out.println(" FOUND " + vehicles.size() + " CARS");

            for (Vehicle v : vehicles) {
                System.out.println("\nConnecting to " + v + " @ " + v.getAddress());
                v.connect();
                System.out.print("   Connected. Setting SDK mode...");   //always set the SDK mode FIRST!
                v.sendMessage(new SdkModeMessage());
                System.out.println("   SDK Mode set.");

                System.out.println("   Flashing lights...");
                LightConfig lc = new LightConfig(LightChannel.ENGINE_RED, LightEffect.FADE,
                        100, 10, 0);
                LightsPatternMessage lpm = new LightsPatternMessage();
                lpm.add(lc);
                v.sendMessage(lpm);

                System.out.println("   Setting Speed...");
                v.sendMessage(new SetSpeedMessage(500, 100));

                LocalizationPositionHandler lph = new LocalizationPositionHandler();
                v.addMessageListener(LocalizationPositionUpdateMessage.class, lph);

                System.out.print("  Sleeping for 5secs... ");
                Thread.sleep(5000);

                v.disconnect();
                System.out.println("not disconnected from " + v + "\n");
            }
        }
        anki.close();
        System.exit(0);
    }

    private static class LocalizationPositionHandler implements MessageListener<LocalizationPositionUpdateMessage> {
        @Override
        public void messageReceived(LocalizationPositionUpdateMessage m) {
            System.out.println("" + m.toString() + "");
        }
    }
}
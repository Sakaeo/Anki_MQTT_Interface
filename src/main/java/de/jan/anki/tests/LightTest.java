package de.jan.anki.tests;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.*;

import java.io.IOException;
import java.util.List;

/**
 * Some light Test.
 * Strübin Jan, 11.06.20
 */
public class LightTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Launching connector...");
        AnkiConnector anki = new AnkiConnector("169.254.28.205", 5000);
        System.out.print("...looking for cars...");
        List<Vehicle> vehicles = anki.findVehicles();

        String testCar = "ec4a0";

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

            System.out.println("   Flashing lights...");
            LightsPatternMessage.LightConfig lc = new LightsPatternMessage.LightConfig(LightsPatternMessage.LightChannel.ENGINE_GREEN, LightsPatternMessage.LightEffect.STEADY, 100, 0, 0);
            LightsPatternMessage lpm = new LightsPatternMessage();
            lpm.add(lc);
            v.sendMessage(lpm);

            System.out.println("Enter to cancel");
            System.out.println(System.in.read());

            v.disconnect();
            System.out.println("not disconnected from " + v + "\n");
        }

        anki.close();
        System.exit(0);
    }
}

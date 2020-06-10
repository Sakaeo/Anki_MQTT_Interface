package de.jan.anki.host;

import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.BatteryLevelRequestMessage;
import de.adesso.anki.messages.PingRequestMessage;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class checks if a car is online or not.
 * Updates car status and removes Car if offline.
 * Strübin Jan, 11.06.20
 */
public class CarPing extends TimerTask {
    private final MQTTPublisher carPublisher;
    private final Vehicle v;
    private final AnkiHost host;
    private final Timer timer;

    /**
     * @param carPublisher Mqtt publisher for the specific car
     * @param v            Car to be checked if online or not
     * @param host         AnkiHost instance in which this class is running
     * @param timer        Timer instance that manages this TimerTask
     */
    public CarPing(MQTTPublisher carPublisher, Vehicle v, AnkiHost host, Timer timer) {
        this.carPublisher = carPublisher;
        this.v = v;
        this.host = host;
        this.timer = timer;
    }

    /**
     * Pings the car, waits 0.3s.
     * If online the result is published on the Mqtt broker and if not the car is removed.
     */
    @Override
    public void run() {
        host.setCarStatus(v, false);

        v.sendMessage(new PingRequestMessage());
//        v.sendMessage(new BatteryLevelRequestMessage()); //TODO: Remove?

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace(); //TODO: Error
        }

        if (host.getCarStatus(v)) {
            JSONObject json = new JSONObject();
            json.put("timestamp", new Timestamp(System.currentTimeMillis()));
            json.put("online", true);
            if (v.getAdvertisement().isCharging()) {
                json.put("charging", true);
            }
            if (v.getAdvertisement().isOnTrack()) {
                json.put("onTrack", true);
            }
            carPublisher.publish("S/CarStatus", json.toString(), true);
        } else {
            host.removeCar(v);
            timer.cancel();
        }
    }
}

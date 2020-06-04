package de.jan.anki.host;

import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.TimerTask;

/**
 * TimerTask that publishes that the AnkiHost is online
 */
public class HostPing extends TimerTask {
    private final MQTTPublisher hostPublisher;

    /**
     * @param hostPublisher Mqtt publisher for the AnkiHost
     */
    public HostPing(MQTTPublisher hostPublisher) {
        this.hostPublisher = hostPublisher;
    }

    /**
     * Publishes a true JSON to the host status
     */
    @Override
    public void run() {
        JSONObject json = new JSONObject();
        json.put("timestamp", new Timestamp(System.currentTimeMillis()));
        json.put("value", true);
        hostPublisher.publish("S/HostStatus", json.toString(), true);
    }
}

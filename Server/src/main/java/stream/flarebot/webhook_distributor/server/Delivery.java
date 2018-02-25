package stream.flarebot.webhook_distributor.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class Delivery {

    private String ip;
    private String userAgent;
    private JsonElement payload;

    Delivery(String ip, String userAgent, JsonElement payload) {
        this.ip = ip;
        this.userAgent = userAgent;
        this.payload = payload;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public boolean isBatch() {
        return payload instanceof JsonArray && ((JsonArray) payload).size() > 1;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }
}

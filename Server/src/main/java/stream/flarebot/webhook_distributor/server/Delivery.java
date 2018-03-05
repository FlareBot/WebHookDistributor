package stream.flarebot.webhook_distributor.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class Delivery {

    private String ip;
    private String userAgent;
    private String authorization;
    private JsonElement payload;

    Delivery(String ip, String userAgent, String authorization, JsonElement payload) {
        this.ip = ip;
        this.userAgent = userAgent;
        this.authorization = authorization;
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

    public String getAuthorization() {
        return authorization;
    }

    public boolean isBatch() {
        return payload instanceof JsonArray && ((JsonArray) payload).size() > 1;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }
}

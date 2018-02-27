package stream.flarebot.webhook_distributor.events;

import com.google.gson.JsonElement;
import spark.Request;

public class WebHookReceiveEvent extends Event {

    private JsonElement payload;

    public WebHookReceiveEvent(JsonElement element, Request request) {
        super(request);
        this.payload = element;
    }

    public WebHookReceiveEvent(JsonElement element, Event event) {
        super(event.getRequest());
        this.payload = element;
    }

    public JsonElement getPayload() {
        return payload;
    }
}

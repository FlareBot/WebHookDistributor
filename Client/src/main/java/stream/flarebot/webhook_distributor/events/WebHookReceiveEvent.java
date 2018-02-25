package stream.flarebot.webhook_distributor.events;

import com.google.gson.JsonElement;
import stream.flarebot.webhook_distributor.Sender;

public class WebHookReceiveEvent extends Event {

    private JsonElement payload;

    public WebHookReceiveEvent(JsonElement element, Sender sender) {
        super(sender);
        this.payload = element;
    }

    public JsonElement getPayload() {
        return payload;
    }
}

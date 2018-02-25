package stream.flarebot.webhook_distributor.events;

import com.google.gson.JsonArray;
import stream.flarebot.webhook_distributor.Sender;

public class WebHookBatchReceiveEvent extends Event {

    private JsonArray webHooks;

    public WebHookBatchReceiveEvent(JsonArray webHooks, Sender sender) {
        super(sender);
        this.webHooks = webHooks;
    }

    public JsonArray getWebHooks() {
        return webHooks;
    }
}

package stream.flarebot.webhook_distributor;

import stream.flarebot.webhook_distributor.events.WebHookBatchReceiveEvent;
import stream.flarebot.webhook_distributor.events.WebHookReceiveEvent;

public abstract class WebHookListener {

    /**
     * This is fired when a webhook is received by the client, this will contain a JsonElement payload and an
     * indication of who sent it with a {@link Sender}.
     *
     * @param e The {@link WebHookReceiveEvent} being fired.
     */
    public void onWebHookReceive(WebHookReceiveEvent e) {}

    /**
     * This is fired when a batch of webhooks is received by the client, this will contain a JsonArray payload of all
     * the webhooks and an indication of who sent it with a {@link Sender}.
     *
     * @param e The {@link WebHookReceiveEvent} being fired.
     */
    public void onBatchWebHookReceive(WebHookBatchReceiveEvent e) {}
}

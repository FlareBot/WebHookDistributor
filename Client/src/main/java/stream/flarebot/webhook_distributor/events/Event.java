package stream.flarebot.webhook_distributor.events;

import spark.Request;
import stream.flarebot.webhook_distributor.Sender;

import javax.annotation.Nullable;

public class Event {

    private Request request;
    private Sender sender;

    Event(Request request) {
        this.request = request;
        this.sender = Sender.getSender(request);
    }

    public Sender getSender() {
        return sender;
    }

    @Nullable
    public String getAuthorization() {
        return this.request.headers("Authorization");
    }

    protected Request getRequest() {
        return this.request;
    }
}

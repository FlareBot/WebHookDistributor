package stream.flarebot.webhook_distributor.events;

import stream.flarebot.webhook_distributor.Sender;

public class Event {

    private Sender sender;

    Event(Sender sender) {
        this.sender = sender;
    }

    public Sender getSender() {
        return sender;
    }
}

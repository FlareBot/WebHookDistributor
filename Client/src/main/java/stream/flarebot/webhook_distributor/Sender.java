package stream.flarebot.webhook_distributor;

import org.slf4j.LoggerFactory;
import spark.Request;

import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum Sender {

    GITHUB(request -> request.headers().stream().anyMatch(header -> header.startsWith("X-GitHub"))),
    SENTRY(request -> request.userAgent().toLowerCase().startsWith("sentry")),
    POSTMAN(request -> request.userAgent().toLowerCase().contains("postman")),
    DBL(request -> request.userAgent().equals("DBL")),
    UNKNOWN(noOp -> {
        LoggerFactory.getLogger(Sender.class).warn("Found unknown sender, IP: " + noOp.ip() + ", userAgent: "
                + noOp.userAgent() + ", Headers: " + noOp.headers().stream()
                .map(header -> header + ": " + noOp.headers(header)).collect(Collectors.joining(", ")));
        return true;
    });

    private static final Sender[] values = values();
    private Predicate<Request> requestPredicate;
    private String userAgent;

    Sender(Predicate<Request> requestPredicate) {
        this.requestPredicate = requestPredicate;
    }

    public static Sender getSender(Request request) {
        Sender sender = null;
        for (Sender s : values) {
            if (s.requestPredicate.test(request)) {
                sender = s;
                break;
            }
        }
        if (sender == null)
            sender = Sender.UNKNOWN;
        sender.setUserAgent(request.userAgent());
        return sender;
    }
    
    private void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getUserAgent() {
        return this.userAgent;
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}

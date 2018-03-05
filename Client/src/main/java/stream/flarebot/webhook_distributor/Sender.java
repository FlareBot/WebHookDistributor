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
        this.userAgent = request.userAgent();
        for (Sender sender : values) {
            if (sender.requestPredicate.test(request))
                return sender;
        }
        return Sender.UNKNOWN;
    }
    
    public String getUserAgent() {
        return requestPredicate.
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}

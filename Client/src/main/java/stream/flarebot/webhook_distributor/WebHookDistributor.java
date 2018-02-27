package stream.flarebot.webhook_distributor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.Spark;
import stream.flarebot.webhook_distributor.events.WebHookBatchReceiveEvent;
import stream.flarebot.webhook_distributor.events.Event;
import stream.flarebot.webhook_distributor.events.WebHookReceiveEvent;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.Set;

public class WebHookDistributor {

    private final JsonParser parser = new JsonParser();
    private final Logger logger = LoggerFactory.getLogger(WebHookDistributor.class);

    private final String webHookServerUrl;

    private final String serviceName;
    private final int port;
    private boolean usingBatch;

    private int maxConnectionAttempts;
    private int attempts = 0;
    private long retryTime;

    private Set<WebHookListener> listeners;

    /**
     * Start the setup for the WebHookDistributor with the WebHook Server URL and this service name.
     *
     * @param webHookServerUrl THis is the main URL that webhooks get sent to, this is the URL which the server is located at.
     * @param serviceName      This service name.
     * @param port             This is the port this server will run on and receive webhooks from the main server. Set this to -1
     *                         for the client to just find a random port.
     */
    WebHookDistributor(String webHookServerUrl, String serviceName, int port, boolean useBatch,
                              int maxConnectionAttempts, long startingRetryTime, Set<WebHookListener> listeners) {
        this.webHookServerUrl = webHookServerUrl;
        this.serviceName = serviceName;
        this.port = port;
        this.usingBatch = useBatch;
        this.maxConnectionAttempts = maxConnectionAttempts;
        this.retryTime = startingRetryTime;
        this.listeners = listeners;
    }

    private void init() {
        Spark.port(port);
        Spark.init();
        Spark.awaitInitialization();
        logger.info("Started WebHook Listening server on port " + Spark.port());
    }

    /**
     * Start the WebHookDistributor, this will try and connect to the main WebHookDistributor Server and see if it's alive.
     * After that it will send the port and the service name so the server can expect and redirect the webhooks here.
     */
    public void start() {
        init();
        checkServer();
        setupRoutes();
    }

    private void checkServer() {
        OkHttpClient client = new OkHttpClient();
        try {
            com.squareup.okhttp.Response response = client.newCall(new Request.Builder().url(webHookServerUrl + "/" + serviceName + "/init")
                    .addHeader("User-Agent", "WebHookDistributor")
                    .put(RequestBody.create(MediaType.parse("application/json"), "{\"port\": " + port + "}"))
                    .build()).execute();

            if (response.code() != 200) {
                logger.error("Failed to initialize service! Returned: " + response.code());
                return;
            }
            logger.info("Successfully connected to the server, waiting for webhooks!");
        } catch (IOException e) {
            if (attempts < maxConnectionAttempts) {
                logger.warn("Failed to init service! Retrying in " + retryTime + "ms");
                try {
                    Thread.sleep(retryTime);
                    attempts++;
                    retryTime *= 2;
                    checkServer();
                } catch (InterruptedException e1) {
                    logger.error("Unexpected exception occurred!", e);
                    System.exit(1);
                }
            } else {
                logger.error("Failed to init service! Check the server is online");
                System.exit(1);
            }
        }
    }

    private void sendEvent(Event e) {
        for (WebHookListener listener : listeners) {
            if (e instanceof WebHookReceiveEvent)
                listener.onWebHookReceive((WebHookReceiveEvent) e);
            else if (e instanceof WebHookBatchReceiveEvent) {
                if (usingBatch)
                    listener.onBatchWebHookReceive((WebHookBatchReceiveEvent) e);
                else
                    for (JsonElement element : ((WebHookBatchReceiveEvent) e).getWebHooks())
                        listener.onWebHookReceive(new WebHookReceiveEvent(element, e));
            }
            else
                try {
                    throw new UnexpectedException("Unexpected event was sent! Event: " + e.getClass().getSimpleName());
                } catch (UnexpectedException e1) {
                    e1.printStackTrace();
                }
        }
    }

    private void setupRoutes() {
        Spark.after((req, res) -> {
            res.header("Content-Type", "application/json");
            res.header("Content-Encoding", "gzip");

            logger.info(String.format("[%d] %s Request from %s (%s) to %s",
                    res.status(), req.requestMethod(), req.ip(), req.userAgent(), req.uri()));
        });

        Spark.notFound((req, res) -> {
            res.type("application/json");
            if (req.requestMethod().equalsIgnoreCase("GET"))
                return "{\"Hello\": \"World\"}";
            return "{\"error\":\"Route not found!\"}";
        });

        Spark.get("/", (req, res) -> "{\"Hello\": \"World\"}");

        Spark.post("/", (req, res) -> {
            if (req.body() != null && !req.body().isEmpty()) {
                try {
                    JsonElement element = parser.parse(req.body());

                    sendEvent(new WebHookReceiveEvent(element, req));
                    return getSuccessRequest(res);
                } catch (JsonParseException e) {
                    return getBadRequest(res, "Invalid JSON object!");
                }
            }
            return getBadRequest(res, "Body required.");
        });

        Spark.post("/batch", (req, res) -> {
            if (req.body() != null && !req.body().isEmpty()) {
                try {
                    JsonElement element = parser.parse(req.body());
                    if (!(element instanceof JsonArray))
                        return getBadRequest(res, "Batch needs to send a JsonArray!");

                    sendEvent(new WebHookBatchReceiveEvent(element.getAsJsonArray(), req));
                    return getSuccessRequest(res);
                } catch (JsonParseException e) {
                    return getBadRequest(res, "Invalid JSON object!");
                }
            }
            return getBadRequest(res, "Body required.");
        });

        Spark.get("/ping", (req, res) -> getPingRequest(res));
        logger.info("Setup routes");
    }

    private Response getBadRequest(Response res, String s) {
        res.status(400);
        res.body(String.format("{\"error\": \"Bad request. %s\"}", s));
        return res;
    }

    private Response getSuccessRequest(Response res) {
        res.status(200);
        res.body("{\"message\": \"WebHook received!\"}");
        return res;
    }

    private Response getPingRequest(Response res) {
        res.status(200);
        res.body("{\"isUp\": true}");
        return res;
    }
}

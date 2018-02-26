package stream.flarebot.webhook_distributor.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.Spark;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DistributorServer {

    private final Logger logger = LoggerFactory.getLogger(DistributorServer.class);
    private final JsonParser parser = new JsonParser();
    private final OkHttpClient client = new OkHttpClient();

    private int serverPort;

    private Map<String, Integer> services = new HashMap<>();
    private Map<String, Delivery> queuedDeliveries = new HashMap<>();

    public static void main(String[] args) {
        new DistributorServer().init(args);
    }

    private void init(String[] args) {
        if (args.length > 0) {
            try {
                serverPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Failed to parse port, input: " + args[0]);
                System.exit(1);
            }
        } else {
            File config = new File("config.prop");
            if (config.exists()) {
                try {
                    FileReader fr = new FileReader(config);
                    Properties properties = new Properties();
                    properties.load(fr);

                    serverPort = Integer.parseInt(properties.getProperty("port"));
                } catch (IOException e) {
                    logger.error("Failed to load 'config.prop' file, exiting!");
                    System.exit(1);
                } catch (NumberFormatException e) {
                    logger.error("Failed to parse port!");
                    System.exit(1);
                }
            } else {
                logger.error("No args or file specified, cannot get port!");
                System.exit(1);
            }
        }
        if (serverPort < 2000)
            throw new IllegalStateException("Make sure the port is above 2000!");

        client.setConnectTimeout(1, TimeUnit.SECONDS);

        setupRoutes();

        logger.info("Started! Waiting for services on port " + serverPort);
    }

    private void setupRoutes() {
        Spark.port(this.serverPort);
        Spark.init();
        Spark.awaitInitialization();

        Spark.after((req, res) -> {
            res.header("Content-Encoding", "gzip");
            res.header("Content-Type", "application/json");

            logger.info("[" + res.status() + "] Request from " + req.ip() + " (" + req.userAgent() + ") to " + req.uri());
        });

        Spark.get("/", (req, res) -> "Hello, World");

        Spark.post("/:service/init", (req, res) -> {
            String serviceName = req.params(":service");
            int port;
            if (req.body() == null || req.body().isEmpty())
                return getBadRequest(res, "No body specified!");

            JsonElement element = parser.parse(req.body());
            if (!(element instanceof JsonObject))
                return getBadRequest(res, "Expecting JsonObject!");

            if (element.getAsJsonObject().has("port")) {
                try {
                    port = Integer.parseInt(element.getAsJsonObject().get("port").getAsString());
                } catch (NumberFormatException e) {
                    return getBadRequest(res, "Port is not a number!");
                }
            } else
                return getBadRequest(res, "No port specified!");

            if (port < 2000)
                return getBadRequest(res, "Port must be above 2000!");

            if (!checkIfServiceIsUp(port))
                return getBadRequest(res, "Hey! This isn't a valid service! Stop trying to force start it :(");

            services.put(serviceName, port);
            logger.info("Setup service: " + serviceName + ":" + port);

            if (queuedDeliveries.containsKey(serviceName) && sendDelivery(serviceName))
                logger.info("Successfully sent queued deliveries for new service!");

            return getSuccessRequest(res, "Setup service '" + serviceName + "' on port '" + port + "'");
        });

        Spark.post("/:service", (req, res) -> {
            String serviceName = req.params(":service");

            if (req.body() == null || req.body().isEmpty())
                return getBadRequest(res, "No body specified!");

            JsonElement element = parser.parse(req.body());
            Delivery delivery = new Delivery(req.ip(), req.userAgent(), element);

            if (!services.containsKey(serviceName)) {
                logger.warn("Got webhook event for non-active service. Queued webhook");
                queueDelivery(serviceName, delivery);
                return getQueuedWebhookRequest(res);
            }
            return sendDelivery(serviceName, delivery) ? getSuccessRequest(res, "Sent webhook!")
                    : getBadRequest(res, "Malformed request JSON! Check the server logs!");
        });
    }

    private boolean checkIfServiceIsUp(int port) {
        try {
            com.squareup.okhttp.Response response = client.newCall(new Request.Builder()
                    .url("http://localhost:" + port + "/ping")
                    .get()
                    .build()).execute();

            return response.code() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private Response getBadRequest(Response res, String s) {
        res.status(400);
        res.body(String.format("{\"error\": \"Bad request. %s\"}", s));
        return res;
    }

    private Response getSuccessRequest(Response res, String s) {
        res.status(200);
        res.body(String.format("{\"message\": \"%s\"}", s));
        return res;
    }

    private Response getQueuedWebhookRequest(Response res) {
        res.status(202);
        res.body("{\"message\": \"Non-active service. Queued webhook.\"}");
        return res;
    }

    private void queueDelivery(String serviceName, Delivery delivery) {
        if (queuedDeliveries.containsKey(serviceName)) {
            Delivery queuedDelivery = queuedDeliveries.get(serviceName);

            JsonArray array = queuedDelivery.getPayload().isJsonArray() ? queuedDelivery.getPayload().getAsJsonArray()
                    : new JsonArray();
            array.add(delivery.getPayload());
            delivery.setPayload(array);
        }
        queuedDeliveries.put(serviceName, delivery);
    }

    private boolean sendDelivery(String service) {
        if (!services.containsKey(service) && !queuedDeliveries.containsKey(service))
            return false;

        Delivery delivery = this.queuedDeliveries.get(service);
        this.queuedDeliveries.remove(service);
        return sendDelivery(service, delivery);
    }

    /**
     * Send a JsonElement payload to the service.
     *
     * @param service Service to send the payload to.
     * @param delivery The payload to send, use a JsonObject or JsonArray here.
     * @return Returns if the delivery was a success.
     */
    private boolean sendDelivery(String service, Delivery delivery) {
        if (!services.containsKey(service))
            return false;

        String url = "http://localhost:" + services.get(service) + (delivery.isBatch() ? "/batch" : "/");
        logger.info("Sending delivery to " + url);

        try {
            com.squareup.okhttp.Response response = client.newCall(new Request.Builder().url(url)
                    .addHeader("User-Agent", delivery.getUserAgent())
                    .addHeader("Sent-By", delivery.getIp())
                    .post(RequestBody.create(MediaType.parse("application/json"), delivery.getPayload().toString()))
                    .build()).execute();

            if (response.code() != 200) {
                ResponseBody body = response.body();
                logger.error("Failed to send a successful webhook! Code: " + response.code() + ", Message: "
                        + response.message() + ", JSON: " + body.string()
                        + "\nOur JSON: " + delivery.getPayload().toString());
                body.close();
                return false;
            }
            return true;
        } catch (IOException e) {
            if (e.getMessage().toLowerCase().contains("failed to connect")) {
                logger.error("Failed to connect to the client, queued WebHook!");
                this.services.remove(service); // It is likely down, so let's remove it.
            } else
                logger.error("Failed to send request, queued WebHook! Message: " + e.getMessage());
            queueDelivery(service, delivery);
            return false;
        }
    }
}

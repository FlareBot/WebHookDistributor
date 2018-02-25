package stream.flarebot.webhook_distributor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class WebHookDistributorBuilder {

    private String webHookServerUrl;

    private String serviceName;
    private int port;
    private boolean usingBatch = false;

    private int maxConnectionAttempts = 3;
    private long retryTime = 2000;

    private Set<WebHookListener> listeners = new HashSet<>();

    public WebHookDistributorBuilder(String webHookServerUrl, String serviceName, int port) {
        Objects.requireNonNull(webHookServerUrl, "The WebHookDistributor Server URL must not be null!");
        Objects.requireNonNull(webHookServerUrl, "The service name must not be null!");
        if (port < 2000)
            throw new IllegalArgumentException("The port must be above 2000!");
        this.webHookServerUrl = webHookServerUrl;
        this.serviceName = serviceName;
        this.port = port;
    }

    /**
     * Add an event listener to the distributor.
     *
     * @param webHookListener The WebHookListener to add to the distributor.
     * @return WebHookDistributorBuilder - Useful for chaining.
     */
    public WebHookDistributorBuilder addEventListener(WebHookListener webHookListener) {
        this.listeners.add(webHookListener);
        return this;
    }

    /**
     * Add an event listener to the distributor.
     *
     * @param webHookListeners A Set of WebHookListener classes to add to the distributor.
     * @return WebHookDistributorBuilder - Useful for chaining.
     */
    public WebHookDistributorBuilder addEventListeners(Set<WebHookListener> webHookListeners) {
        this.listeners.addAll(webHookListeners);
        return this;
    }

    /**
     * This sets the max amount of connection attempts with the server. The wait time between attempts is exponential.
     * The default maximum attempts is 3.
     *
     * @param maxConnectionAttempts The amount of attempts before the program gives up connecting to the server.
     * @return WebHookDistributorBuilder - Useful for chaining.
     */
    public WebHookDistributorBuilder setMaxConnectionAttempts(int maxConnectionAttempts) {
        this.maxConnectionAttempts = maxConnectionAttempts;
        return this;
    }

    /**
     * Set the WebHookDistributor to use batch events instead of sending all events when the client misses some.
     *
     * @return WebHookDistributorBuilder - Useful for chaining.
     */
    public WebHookDistributorBuilder useBatch() {
        this.usingBatch = true;
        return this;
    }

    /**
     * If the WebHookDistributor should be using the {@link stream.flarebot.webhook_distributor.events.WebHookBatchReceiveEvent}
     * which will send all the missed events as one rather than firing the usual
     * {@link stream.flarebot.webhook_distributor.events.WebHookReceiveEvent} multiple times.
     *
     * @param useBatch If the WebHookDistributor should be using the batch event.
     * @return WebHookDistributorBuilder - Useful for chaining.
     */
    public WebHookDistributorBuilder useBatch(boolean useBatch) {
        this.usingBatch = useBatch;
        return this;
    }

    /**
     * Set the starting retry time which is exponentially increased each fail. Time is in milliseconds.
     *
     * @param retryTime Set the amount of milliseconds for the starting retryTime.
     * @return WebHookDistributorBuilder - Useful for chaining.
     */
    public WebHookDistributorBuilder setStartingRetryTime(long retryTime) {
        this.retryTime = retryTime;
        return this;
    }

    public WebHookDistributor build() {
        return new WebHookDistributor(webHookServerUrl, serviceName, port, usingBatch, maxConnectionAttempts, retryTime,
                listeners);
    }
}

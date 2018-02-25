# WebHookDistributor

## What is it?
This is a tool created to distribute web-hooks amongst multiple different clients without the need for exposing multiple ports or making multiple sub-domains. This was created for people who either don't want to expose multiple ports or can't for some reason or another. This allows them to only have 1 port that is open and actually receiving data, from there it can work on sending that to the client that needs it. The system is all dynamic and doesn't use any set constants.

## How does it work?
Let's say there's 2 clients, #1 is waiting for WebHooks from GitHub and #2 is waiting for WebHooks from Sentry. Assuming the server is up when the clients start they will send a request to the server (This also has a retry ability just incase) telling it the port they want to use. From there the server will cache that client and the port so that it knows to send data when it gets it for that service. Client #1 and #2 both had their service names as what they're waiting for ("github" and "sentry"), when the server gets a request sent to it with the route `/github` it will then take that data which it received, check for a client in this case we have #1 and it will send it over to that client.

## How do I use it?
It's very simple to setup and use, just build the server and run it (Specify the server port in the first program argument or in a `config.prop` file).  
For the clients you can add them to any program by just building the `WebHookDistributor` and running `start()`. You can listen to WebHooks by using the `WebHookListener` and overriding the methods `void onWebHookReceive(WebHookReceiveEvent)` and `void onBatchWebHookReceive(WebHookBatchReceiveEvent)`. Both methods are documented with what they do!  

> Note: To use the batch system you need to add the `useBatch()` method to the builder.

Example class:  
```java
import org.slf4j.LoggerFactory;
import stream.flarebot.webhook_distributor.WebHookDistributor;
import stream.flarebot.webhook_distributor.WebHookDistributorBuilder;
import stream.flarebot.webhook_distributor.WebHookListener;
import stream.flarebot.webhook_distributor.events.WebHookReceiveEvent;

public class Example {

    public static void main(String[] args) {
    	// This will say that the server is located at "https://cool-webhooks.flarebot.stream", the service is called "example" and the port for this service is '8181'.
        WebHookDistributor distributor = new WebHookDistributorBuilder("https://cool-webhooks.flarebot.stream", "example", 8181)
        		// This will add the listener which is defined below.
                .addEventListener(new Listener())
                // This is the starting retry time, when connection to the server fails it will use this value first and double each failed attempt.
                .setStartingRetryTime(500)
                // The amount of connection attempts to make to the server before giving up.
                .setMaxConnectionAttempts(5)
                .build();
		// Start listening to WebHooks from the server.
        distributor.start();
    }

    private static class Listener extends WebHookListener {

        public void onWebHookReceive(WebHookReceiveEvent e){
            LoggerFactory.getLogger(Example.class).info(String.format("Received webhook from %s, data: %s",
                    e.getSender().toString(), e.getPayload().toString()));
        }
    }
}
```
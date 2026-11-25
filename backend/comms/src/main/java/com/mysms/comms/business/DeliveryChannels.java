package com.mysms.comms.business;

import com.mysms.comms.errors.NotConfiguredException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The delivery channels this deployment offers, by name. A channel without a provider (SMS and push until one is
 * built) is 503 NOT_CONFIGURED, before anything is written.
 */
@Component
public class DeliveryChannels {

    private final List<DeliveryChannel> channels;

    public DeliveryChannels(List<DeliveryChannel> channels) {
        this.channels = List.copyOf(channels);
    }

    /** The channel, configured and ready; 503 NOT_CONFIGURED otherwise. */
    public DeliveryChannel require(String name) {
        DeliveryChannel channel = channels.stream().filter(candidate -> candidate.name().equals(name)).findFirst()
                .orElseThrow(() -> new NotConfiguredException("No " + name + " provider is configured for this "
                        + "deployment."));
        channel.requireConfigured();
        return channel;
    }
}

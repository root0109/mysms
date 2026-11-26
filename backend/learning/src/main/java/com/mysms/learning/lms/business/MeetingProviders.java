package com.mysms.learning.lms.business;

import java.util.List;
import org.springframework.stereotype.Component;

/** Picks the adapter of a provider code; a provider without a configured adapter is 503 CONFERENCING_NOT_CONFIGURED. */
@Component
public class MeetingProviders {

    private final List<MeetingProvider> providers;

    public MeetingProviders(List<MeetingProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public MeetingProvider of(String provider) {
        return providers.stream().filter(adapter -> adapter.provider().equals(provider) && adapter.configured())
                .findFirst().orElseThrow(() -> new ConferencingNotConfiguredException(provider));
    }
}

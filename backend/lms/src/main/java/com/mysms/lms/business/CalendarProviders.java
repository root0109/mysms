package com.mysms.lms.business;

import java.util.List;
import org.springframework.stereotype.Component;

/** The configured calendar adapters; asking for one that is absent or unconfigured is 503. */
@Component
public class CalendarProviders {

    private final List<CalendarProvider> providers;

    public CalendarProviders(List<CalendarProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public CalendarProvider of(String provider) {
        return providers.stream().filter(adapter -> adapter.provider().equals(provider) && adapter.configured())
                .findFirst().orElseThrow(() -> new CalendarNotConfiguredException(provider));
    }
}

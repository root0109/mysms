package com.mysms.platform.config;

import com.mysms.platform.business.StockImageLookup;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * The stock-photo provider behind platform.SearchStockImages (Pexels): the API key from
 * {@code mysms.platform.stock-images.api-key}, else {@code MYSMS_PEXELS_API_KEY}; without one the search answers 503
 * NOT_CONFIGURED. {@code mysms.platform.stock-images.url} / {@code MYSMS_PEXELS_URL} overrides the endpoint.
 */
@Configuration(proxyBeanMethods = false)
public class PlatformStockImagesConfig {

    @Bean
    public StockImageLookup stockImageLookup(
            @Value("${mysms.platform.stock-images.url}") String url,
            @Value("${mysms.platform.stock-images.api-key}") String apiKey,
            JsonMapper json) {
        return new StockImageLookup(url.isBlank() ? StockImageLookup.DEFAULT_BASE_URI : URI.create(url),
                apiKey.isBlank() ? null : apiKey, Duration.ofSeconds(10), json);
    }
}

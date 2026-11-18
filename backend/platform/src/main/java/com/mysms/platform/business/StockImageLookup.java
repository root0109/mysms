package com.mysms.platform.business;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.platform.api.model.StockImage;
import com.mysms.platform.errors.NotConfiguredException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * platform.SearchStockImages through Pexels' search API (the Adapter between the contract's StockImage and the
 * provider's photo entity): the large rendition as the image, the medium one as its thumbnail, the photographer as
 * the author the client credits. Without an API key ({@code mysms.platform.stock-images.api-key} /
 * {@code MYSMS_PEXELS_API_KEY}) every search is 503 NOT_CONFIGURED; the provider unreachable or failing is 503
 * UNAVAILABLE, and its own message never reaches the client. Pexels serves at most 80 photos a page.
 */
public final class StockImageLookup {

    /** The provider's production endpoint. */
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.pexels.com/v1/");
    static final int PROVIDER_MAX_PAGE = 80;
    private static final int MAX_QUERY = 100;
    private static final String UNREACHABLE = "The stock-photo service could not answer.";

    private final URI baseUri;
    private final String apiKey;
    private final Duration timeout;
    private final HttpClient http;
    private final JsonMapper json;

    /** @param apiKey the provider key, or null when this deployment configures none */
    public StockImageLookup(URI baseUri, String apiKey, Duration timeout, JsonMapper json) {
        this.baseUri = baseUri.toString().endsWith("/") ? baseUri : URI.create(baseUri + "/");
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /** One page of photos matching the text; the next page exists when the provider says so. */
    public Page<StockImage> search(String query, Integer pageNumber, Integer pageSize) {
        if (apiKey == null) {
            throw new NotConfiguredException("Stock-photo search is not configured for this deployment.");
        }
        String text = query.trim();
        if (text.isEmpty() || text.length() > MAX_QUERY) {
            throw new InvalidArgumentException("query: 1 to " + MAX_QUERY + " characters.");
        }
        int page = pageNumber == null ? 1 : pageNumber;
        int size = Math.min(pageSize == null ? PageRequest.DEFAULT_SIZE : pageSize, PROVIDER_MAX_PAGE);
        URI uri = baseUri.resolve("search?query=" + URLEncoder.encode(text, StandardCharsets.UTF_8) + "&page=" + page
                + "&per_page=" + size);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).header("Authorization", apiKey)
                .header("Accept", "application/json").GET().build();
        JsonNode answer = read(send(request));
        List<StockImage> images = new ArrayList<>();
        for (JsonNode photo : answer.path("photos")) {
            images.add(new StockImage(photo.path("src").path("large").asString(""),
                    photo.path("src").path("medium").asString("")).author(photo.path("photographer").asString(null)));
        }
        boolean more = answer.hasNonNull("next_page");
        return new Page<>(images, page, size, more ? "p" + (page + 1) : "");
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new UnavailableException(UNREACHABLE);
            }
            return response;
        } catch (IOException unreachable) {
            throw new UnavailableException(UNREACHABLE);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new UnavailableException(UNREACHABLE);
        }
    }

    private JsonNode read(HttpResponse<String> response) {
        try {
            return json.readTree(response.body());
        } catch (JacksonException notJson) {
            throw new UnavailableException(UNREACHABLE);
        }
    }
}

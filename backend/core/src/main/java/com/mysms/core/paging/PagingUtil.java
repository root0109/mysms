package com.mysms.core.paging;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Turns a repository {@link Page} into the {@link PagingInfo} a list operation returns. Links are the request's own
 * relative path and query with only {@code page}, {@code size} and {@code cursor} replaced, so filter, sort and mask
 * carry over; {@code previous} is omitted beyond {@link PageRequest#MAX_OFFSET} and {@code next} on the last page.
 */
public final class PagingUtil {

    private static final Set<String> PAGING_PARAMETERS = Set.of("page", "size", "cursor");

    private PagingUtil() {
    }

    /** The response of a list operation for the page read, with links built from the current request. */
    public static <T> PagingInfo<T> pagingInfo(Page<T> page) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return pagingInfo(page, request.getRequestURI(), request.getQueryString());
    }

    /**
     * The response of a list operation for the page read, with links built from the given request line.
     *
     * @param path     the request path, e.g. {@code /v1/hr/staff}
     * @param rawQuery the request's query string as received (still URL-encoded), or null
     */
    public static <T> PagingInfo<T> pagingInfo(Page<T> page, String path, String rawQuery) {
        List<String> kept = keptParameters(rawQuery);
        int number = page.page();
        int size = page.size();
        String previous = number > 1 && PageRequest.reachableByOffset(number - 1, size)
                ? link(path, kept, number - 1, size, null)
                : null;
        String next = page.hasNext() ? link(path, kept, number + 1, size, page.nextCursor()) : null;
        NavigationInfo navigation = new NavigationInfo(number, page.items().size(),
                link(path, kept, 1, size, null), previous, next);
        return new PagingInfo<>(page.items(), navigation);
    }

    /**
     * The cursor a {@code next} link carries, for a caller that follows the pages of another module's list (the
     * request for the next page sends it with {@code page} + 1); empty when there is no next page.
     *
     * @param next the {@code next} link of a {@link NavigationInfo}, or null on the last page
     */
    public static Optional<String> cursorOf(String next) {
        if (next == null) {
            return Optional.empty();
        }
        int query = next.indexOf('?');
        for (String parameter : query < 0 ? new String[0] : next.substring(query + 1).split("&")) {
            if (parameter.startsWith("cursor=")) {
                String cursor = URLDecoder.decode(parameter.substring("cursor=".length()), StandardCharsets.UTF_8);
                return cursor.isEmpty() ? Optional.empty() : Optional.of(cursor);
            }
        }
        return Optional.empty();
    }

    /** The query's parameters other than page, size and cursor, in their order and original encoding. */
    private static List<String> keptParameters(String rawQuery) {
        List<String> kept = new ArrayList<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return kept;
        }
        for (String parameter : rawQuery.split("&")) {
            if (parameter.isEmpty()) {
                continue;
            }
            int equals = parameter.indexOf('=');
            if (!PAGING_PARAMETERS.contains(name(equals < 0 ? parameter : parameter.substring(0, equals)))) {
                kept.add(parameter);
            }
        }
        return kept;
    }

    private static String name(String encoded) {
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malformed) {
            return encoded; // not a paging parameter's name; kept as received
        }
    }

    private static String link(String path, List<String> kept, int page, int size, String cursor) {
        StringBuilder link = new StringBuilder(path).append("?page=").append(page).append("&size=").append(size);
        for (String parameter : kept) {
            link.append('&').append(parameter);
        }
        if (cursor != null) {
            // base64url without padding: every character is URL-safe as is
            link.append("&cursor=").append(cursor);
        }
        return link.toString();
    }
}

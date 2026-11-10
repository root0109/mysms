package com.mysms.core.error;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * The message catalogue of the API's error sentences (modelled on the owner's {@code common} module): a sentence is
 * looked up by its code in the language of the call — the request's {@code Accept-Language}, which Spring binds to
 * the thread; a request that names no language is given the server's default locale by the servlet container, so a
 * deployment that adds a bundle for the server's own language answers such requests in it — from the bundles {@code
 * messages/mysms*.properties}. The base bundle is English; another language is
 * added by putting {@code messages/mysms_<language>.properties} on the classpath, with no change to code.
 *
 * <p>A code the catalogue does not know keeps the sentence the code that raised the error gave, so the catalogue
 * can grow one code at a time.
 */
public final class MessageUtil {

    /** Where the bundles are, on every module's classpath. */
    public static final String BUNDLE = "messages/mysms";

    private final MessageSource messageSource;

    public MessageUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /** The catalogue over {@link #BUNDLE}; a language without a bundle answers in English. */
    public static MessageUtil ofCatalogue() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename(BUNDLE);
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        return new MessageUtil(source);
    }

    /** The base bundle's (English) sentence for {@code code}; null when the catalogue does not know the code. */
    public String getStandardMessage(String code) {
        return messageSource.getMessage(code, null, null, Locale.ROOT);
    }

    /** The sentence for {@code code} in the caller's language; {@code fallback} when the catalogue has none. */
    public String getMessage(String code, String fallback, Object... params) {
        return messageSource.getMessage(code, params, fallback, LocaleContextHolder.getLocale());
    }
}
